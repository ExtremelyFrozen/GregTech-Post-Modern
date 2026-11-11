package com.gregtechceu.gtceu.client.renderer.preview;

import com.gregtechceu.gtceu.api.multiblock.MultiblockPreviewLevel;

import com.lowdragmc.lowdraglib2.client.scene.ISceneBlockRenderHook;
import com.lowdragmc.lowdraglib2.client.scene.WorldSceneRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.data.ModelData;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.joml.Matrix4f;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Render-thread geometry. Its level and positions remain unchanged until this mesh is closed. */
@OnlyIn(Dist.CLIENT)
@NullMarked
public final class PreviewMesh implements AutoCloseable {

    private final MultiblockPreviewLevel level;
    private final LongList positions;
    private final @Nullable ISceneBlockRenderHook hook;
    private final float scale;
    private final PoseStack pose = new PoseStack();
    private final RandomSource random = RandomSource.create();
    private final BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
    private final @Nullable Layer[] layers = new Layer[RenderType.chunkBufferLayers().size()];
    private final ObjectList<RenderedBlockEntity> blockEntities = new ObjectArrayList<>();
    private int cursor;
    private boolean compiled;
    // The missing model is replaced with the baked model set, including on F3+T reloads.
    private BakedModel models = Minecraft.getInstance().getModelManager().getMissingModel();

    public PreviewMesh(MultiblockPreviewLevel level, LongList positions,
                       @Nullable ISceneBlockRenderHook hook, float scale) {
        this.level = level;
        this.positions = positions;
        this.hook = hook;
        this.scale = scale;
    }

    /** Advances only changed geometry, yielding after a frame's 2 ms compile budget. */
    public boolean compile() {
        var currentModels = Minecraft.getInstance().getModelManager().getMissingModel();
        if (models != currentModels) {
            close();
            models = currentModels;
        }
        if (compiled) return true;
        long deadline = System.nanoTime() + 2_000_000;
        ModelBlockRenderer.enableCaching();
        try {
            while (cursor < positions.size()) {
                compileBlock(position.set(positions.getLong(cursor++)));
                if (System.nanoTime() >= deadline) return false;
            }
            for (var layer : layers) {
                if (layer != null) layer.upload();
            }
            compiled = true;
            return true;
        } finally {
            ModelBlockRenderer.clearCache();
        }
    }

    private void compileBlock(BlockPos pos) {
        var client = Minecraft.getInstance();
        var dispatcher = client.getBlockRenderer();
        var state = level.getBlockState(pos);
        var blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null) {
            var renderer = client.getBlockEntityRenderDispatcher().getRenderer(blockEntity);
            if (renderer != null && blockEntity.getType().isValid(state)) {
                blockEntities.add(new RenderedBlockEntity(blockEntity, renderer));
            }
        }
        if (state.getRenderShape() == RenderShape.MODEL) {
            var model = dispatcher.getBlockModel(state);
            var data = blockEntity == null ? ModelData.EMPTY : blockEntity.getModelData();
            data = model.getModelData(level, pos, state, data);
            long seed = state.getSeed(pos);
            random.setSeed(seed);
            pose.pushPose();
            pose.translate(pos.getX(), pos.getY(), pos.getZ());
            if (scale != 1) {
                pose.translate(0.5, 0.5, 0.5);
                pose.scale(scale, scale, scale);
                pose.translate(-0.5, -0.5, -0.5);
            }
            for (var type : model.getRenderTypes(state, random, data)) {
                var vertices = layer(type).vertices;
                if (hook != null) hook.applyVertexConsumerWrapper(level, pos, state, vertices, type, 0);
                dispatcher.getModelRenderer().tesselateBlock(level, model, state, pos, pose, vertices,
                        scale == 1, random, seed, OverlayTexture.NO_OVERLAY, data, type);
                vertices.clearColor();
            }
            pose.popPose();
        }
        var fluid = state.getFluidState();
        if (!fluid.isEmpty()) {
            var type = ItemBlockRenderTypes.getRenderLayer(fluid);
            var vertices = layer(type).vertices;
            if (hook != null) hook.applyVertexConsumerWrapper(level, pos, state, vertices, type, 0);
            vertices.setOffsetX(pos.getX() & ~15);
            vertices.setOffsetY(pos.getY() & ~15);
            vertices.setOffsetZ(pos.getZ() & ~15);
            dispatcher.renderLiquid(pos, level, vertices, state, fluid);
            vertices.clearOffset();
            vertices.clearColor();
        }
    }

    private Layer layer(RenderType type) {
        int index = type.getChunkLayerId();
        var layer = layers[index];
        if (layer == null) layers[index] = layer = new Layer();
        return layer;
    }

    public void render(Matrix4f modelView, Matrix4f projection, PoseStack poseStack, float partialTick) {
        for (var type : RenderType.chunkBufferLayers()) {
            if (type == RenderType.translucent()) renderBlockEntities(poseStack, partialTick);
            var layer = layers[type.getChunkLayerId()];
            if (layer == null || layer.buffer == null) continue;
            type.setupRenderState();
            try {
                layer.buffer.bind();
                layer.buffer.drawWithShader(modelView, projection, RenderSystem.getShader());
            } finally {
                VertexBuffer.unbind();
                type.clearRenderState();
            }
        }
    }

    private void renderBlockEntities(PoseStack poseStack, float partialTick) {
        var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        for (var entry : blockEntities) {
            var pos = entry.entity.getBlockPos();
            poseStack.pushPose();
            poseStack.translate(pos.getX(), pos.getY(), pos.getZ());
            entry.renderer.render(entry.entity, partialTick, poseStack, buffers,
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
        buffers.endBatch();
    }

    @Override
    public void close() {
        for (int i = 0; i < layers.length; i++) {
            var layer = layers[i];
            if (layer == null) continue;
            if (layer.buffer != null) layer.buffer.close();
            if (!layer.uploaded) layer.memory.close();
            layers[i] = null;
        }
        blockEntities.clear();
        compiled = false;
        cursor = 0;
    }

    private record RenderedBlockEntity(BlockEntity entity, BlockEntityRenderer<BlockEntity> renderer) {}

    private static final class Layer {

        private final ByteBufferBuilder memory = new ByteBufferBuilder(65_536);
        private final BufferBuilder builder = new BufferBuilder(memory, VertexFormat.Mode.QUADS,
                DefaultVertexFormat.BLOCK);
        private final Vertices vertices = new Vertices(builder);
        private @Nullable VertexBuffer buffer;
        private boolean uploaded;

        private void upload() {
            try {
                var mesh = builder.build();
                if (mesh != null) {
                    buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
                    buffer.bind();
                    try {
                        buffer.upload(mesh);
                    } finally {
                        VertexBuffer.unbind();
                    }
                }
            } finally {
                memory.close();
                uploaded = true;
            }
        }
    }

    /** Keep fluent writes on the wrapper, including tint and both light/overlay coordinates. */
    private static final class Vertices extends WorldSceneRenderer.VertexConsumerWrapper {

        private final VertexConsumer target;

        private Vertices(VertexConsumer target) {
            super(target);
            this.target = target;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            super.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            super.setColor(red, green, blue, alpha);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            target.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            target.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            target.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            target.setNormal(x, y, z);
            return this;
        }
    }
}
