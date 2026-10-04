package com.gregtechceu.gtceu.client.renderer;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.multiblock.MultiblockPreviewLevel;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildBatchRequest;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildSharedOptions;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildStructureOptions;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.AutoBuildPlan;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.CellAction;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.MergedPlanCell;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.MultiblockPlanResolver;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.PlannedBlockInfo;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.PlannedCell;
import com.gregtechceu.gtceu.api.multiblock.preview.PatternGenerationGuard;
import com.gregtechceu.gtceu.data.pattern.StructurePatternRegistry;

import com.lowdragmc.lowdraglib2.client.scene.WorldSceneRenderer;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.data.ModelData;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static net.minecraft.world.level.block.RenderShape.INVISIBLE;

@OnlyIn(Dist.CLIENT)
@NullMarked
public final class MultiblockInWorldPreviewRenderer {

    private MultiblockInWorldPreviewRenderer() {}

    private enum CacheState {
        UNUSED,
        COMPILING,
        COMPILED
    }

    private static final Object BUFFER_LOCK = new Object();
    private static final int MAX_PATTERN_PUBLICATION_ATTEMPTS = 8;
    @Nullable
    private static volatile VertexBuffer[] buffers;
    @Nullable
    private static MultiblockPreviewLevel LEVEL = null;
    @Nullable
    private static Thread THREAD = null;
    @Nullable
    private static Level SOURCE_LEVEL;
    @Nullable
    private static Set<BlockPos> BLOCK_ENTITIES;
    @Nullable
    private static Set<BlockPos> LOADED_POSITIONS;
    private final static AtomicInteger LEFT_TICK = new AtomicInteger(-1);
    private static final AtomicLong PREVIEW_GENERATION = new AtomicLong();

    /**
     * Creates one VBO per chunk render layer for the current preview generation.
     */
    private static VertexBuffer[] initBuffers() {
        List<RenderType> layers = RenderType.chunkBufferLayers();
        var buffers = new VertexBuffer[layers.size()];
        for (int j = 0; j < layers.size(); ++j) {
            buffers[j] = new VertexBuffer(VertexBuffer.Usage.STATIC);
        }
        return buffers;
    }

    private final static AtomicReference<CacheState> CACHE_STATE = new AtomicReference<>(CacheState.UNUSED);

    @Nullable
    private static BlockPos LAST_POS = null;
    private static int LAST_LAYER = -1;

    public static void cleanPreview() {
        PREVIEW_GENERATION.incrementAndGet();
        @Nullable
        Thread compiling = THREAD;
        if (compiling != null) {
            compiling.interrupt();
            THREAD = null;
        }
        CACHE_STATE.set(CacheState.UNUSED);
        releasePreviewLevel();
        LEVEL = null;
        SOURCE_LEVEL = null;
        BLOCK_ENTITIES = null;
        LOADED_POSITIONS = null;
        LEFT_TICK.set(-1);
        LAST_POS = null;
        LAST_LAYER = -1;
        closeBuffers();
    }

    public static void removePreview(BlockPos pos) {
        if (LAST_POS != null && LAST_POS.equals(pos)) {
            cleanPreview();
        }
    }

    /**
     * Shows the controller-oriented resolved plan in the world. Repeated use cycles logical pattern layers.
     *
     * @param pos        the pos of the controller
     * @param controller the controller
     * @param duration   the duration of the preview. in ticks.
     */
    public static void showPreview(BlockPos pos, MultiblockControllerMachine controller,
                                   int duration) {
        if (!controller.getDefinition().isRenderWorldPreview()) return;
        @Nullable
        BlockPos previousPos = LAST_POS;
        int previousLayer = LAST_LAYER;
        for (int publicationAttempt = 0; publicationAttempt < MAX_PATTERN_PUBLICATION_ATTEMPTS; publicationAttempt++) {
            MultiblockPlanResolver resolver = new MultiblockPlanResolver();
            PatternGenerationGuard.Resolution<AutoBuildPlan> resolution;
            try {
                resolution = PatternGenerationGuard.resolve(StructurePatternRegistry::generation, attempt -> {
                    AutoBuildBatchRequest request = defaultPreviewRequest(controller, resolver);
                    return resolver.resolveControllerPreview(controller, request);
                });
            } catch (RuntimeException exception) {
                cleanPreview();
                GTCEu.LOGGER.warn("Could not resolve the in-world multiblock preview for {}",
                        controller.getDefinition().getId(), exception);
                return;
            }
            AutoBuildPlan plan = resolution.value();
            if (!plan.sharedProblems().isEmpty() || plan.mergedCells().isEmpty()) {
                cleanPreview();
                return;
            }

            List<Integer> layers = plan.mergedCells().keySet().stream()
                    .map(BlockPos::getY)
                    .distinct()
                    .sorted()
                    .toList();
            int selectedLayer = selectedLayer(previousPos, previousLayer, pos, layers.size());
            MultiblockPreviewLevel previewLevel = new MultiblockPreviewLevel(controller.getLevel());
            Map<BlockPos, PlannedBlockInfo> blockMap = previewBlocks(plan, layers, selectedLayer, previewLevel);
            if (StructurePatternRegistry.generation() != resolution.generation()) {
                releasePreviewLevel(previewLevel, blockMap.keySet());
                continue;
            }

            long previewGeneration = PREVIEW_GENERATION.incrementAndGet();
            @Nullable
            Thread compiling = THREAD;
            if (compiling != null) {
                compiling.interrupt();
                THREAD = null;
            }
            CACHE_STATE.set(CacheState.UNUSED);
            releasePreviewLevel();
            closeBuffers();
            LEVEL = previewLevel;
            SOURCE_LEVEL = controller.getLevel();
            BLOCK_ENTITIES = null;
            LOADED_POSITIONS = Set.copyOf(blockMap.keySet());
            LAST_POS = pos;
            LAST_LAYER = selectedLayer;
            if (StructurePatternRegistry.generation() != resolution.generation()) {
                cleanPreview();
                continue;
            }
            prepareBuffers(previewLevel, blockMap.keySet(), duration, previewGeneration);
            return;
        }
        cleanPreview();
        GTCEu.LOGGER.warn("Pattern generation did not stabilize while publishing the in-world preview for {}",
                controller.getDefinition().getId());
    }

    private static int selectedLayer(@Nullable BlockPos previousPos, int previousLayer, BlockPos pos, int layerCount) {
        if (!pos.equals(previousPos)) return -1;
        int nextLayer = previousLayer + 1;
        return nextLayer >= layerCount ? -1 : nextLayer;
    }

    private static Map<BlockPos, PlannedBlockInfo> previewBlocks(AutoBuildPlan plan, List<Integer> layers,
                                                                 int selectedLayer,
                                                                 MultiblockPreviewLevel previewLevel) {
        Map<BlockPos, PlannedBlockInfo> blockMap = new LinkedHashMap<>();
        for (MergedPlanCell merged : plan.mergedCells().values()) {
            if (selectedLayer != -1 && merged.relativePos().getY() != layers.get(selectedLayer)) {
                continue;
            }
            PlannedCell representative = renderRepresentative(merged);
            @Nullable
            BlockPos worldPos = representative.worldPos();
            if (representative.action() == CellAction.CONTROLLER || worldPos == null ||
                    representative.blockState().isAir() && !merged.conflict()) {
                continue;
            }
            PlannedBlockInfo blockInfo = representative.blockState().isAir() ?
                    PlannedBlockInfo.from(Blocks.WHITE_STAINED_GLASS.defaultBlockState()) :
                    representative.blockInfo();
            blockMap.put(worldPos, blockInfo);
            var previewInfo = blockInfo.createBlockInfo();
            previewLevel.addBlock(worldPos, previewInfo);
            @Nullable
            BlockEntity blockEntity = previewInfo.getBlockEntity(
                    previewLevel.registryAccess(), previewLevel, worldPos);
            if (blockEntity != null) {
                previewLevel.setInnerBlockEntity(blockEntity);
            }
        }
        return blockMap;
    }

    private static PlannedCell renderRepresentative(MergedPlanCell merged) {
        PlannedCell executionRepresentative = merged.representative();
        if (!merged.conflict() || !executionRepresentative.blockState().isAir()) {
            return executionRepresentative;
        }
        List<PlannedCell> contributors = merged.contributors();
        for (int index = contributors.size() - 1; index >= 0; index--) {
            PlannedCell contributor = contributors.get(index);
            if (contributor.action() != CellAction.IGNORE_ANY && !contributor.blockState().isAir()) {
                return contributor;
            }
        }
        return executionRepresentative;
    }

    private static AutoBuildBatchRequest defaultPreviewRequest(MultiblockControllerMachine controller,
                                                               MultiblockPlanResolver resolver) {
        var definition = controller.getDefinition();
        List<String> order = definition.getStructureOrder();
        if (order.isEmpty()) {
            throw new IllegalStateException("Multiblock definition has no structures: " + definition.getId());
        }
        String main = order.contains(MultiblockControllerMachine.DEFAULT_STRUCTURE) ?
                MultiblockControllerMachine.DEFAULT_STRUCTURE : order.getFirst();
        Set<String> selected = new LinkedHashSet<>();
        selected.add(main);
        selected.addAll(definition.getRequiredStructures(main));
        List<AutoBuildStructureOptions> structures = order.stream()
                .filter(selected::contains)
                .map(name -> resolver.defaultStructureOptions(definition, name))
                .toList();
        return new AutoBuildBatchRequest(structures, AutoBuildSharedOptions.DEFAULT, List.of());
    }

    public static void onClientTick() {
        if (SOURCE_LEVEL != null && SOURCE_LEVEL != Minecraft.getInstance().level) {
            cleanPreview();
            return;
        }
        if (LEFT_TICK.get() > 0) {
            if (LEFT_TICK.decrementAndGet() <= 0) {
                cleanPreview();
            }
        }
    }

    public static void renderInWorldPreview(PoseStack poseStack, Camera camera, float partialTicks) {
        @Nullable
        MultiblockPreviewLevel level = LEVEL;
        if (CACHE_STATE.get() == CacheState.COMPILED && level != null) {
            poseStack.pushPose();
            Vec3 projectedView = camera.getPosition();
            poseStack.translate(-projectedView.x, -projectedView.y, -projectedView.z);

            for (int i = 0; i < RenderType.chunkBufferLayers().size(); i++) {
                VertexBuffer vertexbuffer = getBuffers()[i];
                // some of stupid mod doesn't check if the buffer is invalid
                if (vertexbuffer.isInvalid() || vertexbuffer.getFormat() == null) continue;
                var layer = RenderType.chunkBufferLayers().get(i);

                // render TESR before translucent
                @Nullable
                Set<BlockPos> blockEntities = BLOCK_ENTITIES;
                if (layer == RenderType.translucent() && blockEntities != null) { // render tesr before translucent
                    var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
                    for (BlockPos pos : blockEntities) {
                        @Nullable
                        BlockEntity tile = level.getBlockEntity(pos);
                        if (tile != null) {
                            poseStack.pushPose();
                            poseStack.translate(pos.getX(), pos.getY(), pos.getZ());
                            @Nullable
                            BlockEntityRenderer<BlockEntity> ber = Minecraft.getInstance()
                                    .getBlockEntityRenderDispatcher().getRenderer(tile);
                            if (ber != null) {
                                if (tile.hasLevel() && tile.getType().isValid(tile.getBlockState())) {
                                    ber.render(tile, partialTicks, poseStack, buffers, LightTexture.FULL_BRIGHT,
                                            OverlayTexture.NO_OVERLAY);
                                }
                            }
                            poseStack.popPose();
                        }
                    }
                    buffers.endBatch();
                }

                // render cache vbo
                layer.setupRenderState();
                poseStack.pushPose();
                ShaderInstance shaderInstance = RenderSystem.getShader();

                for (int j = 0; j < 12; ++j) {
                    int k = RenderSystem.getShaderTexture(j);
                    shaderInstance.setSampler("Sampler" + j, k);
                }

                // setup shader uniform
                if (shaderInstance.MODEL_VIEW_MATRIX != null) {
                    shaderInstance.MODEL_VIEW_MATRIX.set(poseStack.last().pose());
                }

                if (shaderInstance.PROJECTION_MATRIX != null) {
                    shaderInstance.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());
                }

                if (shaderInstance.COLOR_MODULATOR != null) {
                    shaderInstance.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
                }

                if (shaderInstance.FOG_START != null) {
                    shaderInstance.FOG_START.set(Float.MAX_VALUE);
                }

                if (shaderInstance.FOG_END != null) {
                    shaderInstance.FOG_END.set(RenderSystem.getShaderFogEnd());
                }

                if (shaderInstance.FOG_COLOR != null) {
                    shaderInstance.FOG_COLOR.set(RenderSystem.getShaderFogColor());
                }

                if (shaderInstance.FOG_SHAPE != null) {
                    shaderInstance.FOG_SHAPE.set(RenderSystem.getShaderFogShape().getIndex());
                }

                if (shaderInstance.TEXTURE_MATRIX != null) {
                    shaderInstance.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
                }

                if (shaderInstance.GAME_TIME != null) {
                    shaderInstance.GAME_TIME.set(RenderSystem.getShaderGameTime());
                }

                RenderSystem.setupShaderLights(shaderInstance);
                shaderInstance.apply();

                RenderSystem.setShaderColor(1, 1, 1, 1);
                if (layer == RenderType.translucent()) { // SOLID
                    RenderSystem.enableBlend();
                    RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                    RenderSystem.depthMask(false);
                } else { // TRANSLUCENT
                    RenderSystem.enableDepthTest();
                    RenderSystem.disableBlend();
                    RenderSystem.depthMask(true);
                }

                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

                vertexbuffer.bind();
                vertexbuffer.draw();

                poseStack.popPose();

                shaderInstance.clear();
                VertexBuffer.unbind();
                layer.clearRenderState();
            }
            poseStack.popPose();
        }
    }

    private static void prepareBuffers(MultiblockPreviewLevel level, Collection<BlockPos> renderedBlocks,
                                       int duration, long generation) {
        CACHE_STATE.set(CacheState.COMPILING);
        VertexBuffer[] targetBuffers = getBuffers();
        THREAD = new Thread(() -> {
            var dispatcher = Minecraft.getInstance().getBlockRenderer();
            ModelBlockRenderer.enableCaching();
            try {
                PoseStack poseStack = new PoseStack();
                for (int i = 0; i < RenderType.chunkBufferLayers().size(); i++) {
                    if (stale(generation)) return;
                    var layer = RenderType.chunkBufferLayers().get(i);
                    var buffer = new BufferBuilder(new ByteBufferBuilder(layer.bufferSize()),
                            VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
                    renderBlocks(level, poseStack, dispatcher, layer,
                            new WorldSceneRenderer.VertexConsumerWrapper(buffer), renderedBlocks);
                    var meshData = buffer.buildOrThrow();
                    VertexBuffer vertexBuffer = targetBuffers[i];
                    RenderSystem.recordRenderCall(() -> {
                        try (meshData) {
                            if (stale(generation) || vertexBuffer.isInvalid()) {
                                return;
                            }
                            vertexBuffer.bind();
                            try {
                                vertexBuffer.upload(meshData);
                            } finally {
                                VertexBuffer.unbind();
                            }
                        }
                    });
                }

                Set<BlockPos> poses = new HashSet<>();
                for (BlockPos blockPos : renderedBlocks) {
                    if (stale(generation)) return;
                    @Nullable
                    BlockEntity tile = level.getBlockEntity(blockPos);
                    if (tile != null && Minecraft.getInstance().getBlockEntityRenderDispatcher()
                            .getRenderer(tile) != null) {
                        poses.add(blockPos);
                    }
                }
                if (stale(generation)) return;
                Set<BlockPos> compiledBlockEntities = Set.copyOf(poses);
                RenderSystem.recordRenderCall(() -> {
                    if (stale(generation)) return;
                    BLOCK_ENTITIES = compiledBlockEntities;
                    CACHE_STATE.set(CacheState.COMPILED);
                    LEFT_TICK.set(duration);
                });
            } finally {
                ModelBlockRenderer.clearCache();
                if (PREVIEW_GENERATION.get() == generation) {
                    THREAD = null;
                }
            }
        }, "GTM multiblock preview compiler");
        THREAD.start();
    }

    private static boolean stale(long generation) {
        return Thread.currentThread().isInterrupted() || PREVIEW_GENERATION.get() != generation;
    }

    private static VertexBuffer[] getBuffers() {
        @Nullable
        VertexBuffer[] current = buffers;
        if (current != null) return current;
        synchronized (BUFFER_LOCK) {
            if (buffers == null) {
                buffers = initBuffers();
            }
            return buffers;
        }
    }

    private static void closeBuffers() {
        @Nullable
        VertexBuffer[] old;
        synchronized (BUFFER_LOCK) {
            old = buffers;
            buffers = null;
        }
        if (old == null) return;
        Runnable close = () -> Arrays.stream(old).forEach(VertexBuffer::close);
        if (RenderSystem.isOnRenderThread()) close.run();
        else RenderSystem.recordRenderCall(close::run);
    }

    private static void releasePreviewLevel() {
        @Nullable
        MultiblockPreviewLevel oldLevel = LEVEL;
        @Nullable
        Set<BlockPos> oldPositions = LOADED_POSITIONS;
        if (oldLevel == null || oldPositions == null) return;
        releasePreviewLevel(oldLevel, oldPositions);
    }

    private static void releasePreviewLevel(MultiblockPreviewLevel level, Collection<BlockPos> positions) {
        for (BlockPos blockPos : positions) {
            @Nullable
            BlockEntity blockEntity = level.getBlockEntity(blockPos);
            if (blockEntity != null && !blockEntity.isRemoved()) {
                blockEntity.setRemoved();
            }
        }
    }

    private static void renderBlocks(MultiblockPreviewLevel level, PoseStack poseStack,
                                     BlockRenderDispatcher dispatcher,
                                     RenderType layer, WorldSceneRenderer.VertexConsumerWrapper wrapperBuffer,
                                     Collection<BlockPos> renderedBlocks) {
        for (BlockPos pos : renderedBlocks) {
            BlockState state = level.getBlockState(pos);
            FluidState fluidState = state.getFluidState();
            Block block = state.getBlock();
            if (block == Blocks.AIR) continue;

            // render blocks
            if (state.getRenderShape() != INVISIBLE && ItemBlockRenderTypes.getRenderLayers(state).contains(layer)) {
                poseStack.pushPose();
                poseStack.translate(pos.getX(), pos.getY(), pos.getZ());

                poseStack.translate(0.5, 0.5, 0.5);
                poseStack.scale(0.8f, 0.8f, 0.8f);
                poseStack.translate(-0.5, -0.5, -0.5);

                level.setRenderFilter(p -> p.equals(pos));
                renderBlockLayer(dispatcher, state, pos, level, poseStack, wrapperBuffer, GTValues.RNG, layer);
                level.setRenderFilter(p -> true);
                poseStack.popPose();
            }

            // render fluids
            if (!fluidState.isEmpty() && ItemBlockRenderTypes.getRenderLayer(fluidState) == layer) {
                wrapperBuffer.addOffset((pos.getX() - (pos.getX() & 15)), (pos.getY() - (pos.getY() & 15)),
                        (pos.getZ() - (pos.getZ() & 15)));
                dispatcher.renderLiquid(pos, level, wrapperBuffer, state, fluidState);
            }

            wrapperBuffer.clearOffset();
            wrapperBuffer.clearColor();
        }
    }

    private static void renderBlockLayer(BlockRenderDispatcher dispatcher, BlockState state, BlockPos pos,
                                         MultiblockPreviewLevel level, PoseStack poseStack,
                                         VertexConsumer vertexConsumer,
                                         RandomSource random, RenderType layer) {
        @Nullable
        BlockEntity blockEntity = level.getBlockEntity(pos);
        var model = dispatcher.getBlockModel(state);
        ModelData baseData = blockEntity == null ? ModelData.EMPTY : blockEntity.getModelData();
        ModelData modelData = model.getModelData(level, pos, state, baseData);
        dispatcher.renderBatched(state, pos, level, poseStack, vertexConsumer, false, random, modelData, layer);
    }
}
