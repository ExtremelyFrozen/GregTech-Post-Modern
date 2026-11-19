package com.gregtechceu.gtceu.client.renderer;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.multiblock.MultiblockPreviewLevel;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.BatchRequest;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.StructureRequest;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.BatchPlan;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.BatchPlanner;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.CellAction;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.MergedPlanCell;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.PlannedBlockInfo;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.PlannedCell;
import com.gregtechceu.gtceu.api.multiblock.preview.PatternGenerationGuard;
import com.gregtechceu.gtceu.api.multiblock.preview.StructurePreview;
import com.gregtechceu.gtceu.client.renderer.preview.PreviewMesh;
import com.gregtechceu.gtceu.data.pattern.StructurePatternRegistry;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import org.joml.Matrix4f;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** World overlays share the render-thread mesh compiler with terminal and recipe previews. */
@OnlyIn(Dist.CLIENT)
@NullMarked
public final class MultiblockInWorldPreviewRenderer {

    private static final int MAX_PATTERN_PUBLICATION_ATTEMPTS = 8;
    private static final Matrix4f MODEL_VIEW = new Matrix4f();
    private static final ObjectSet<String> VISIBLE_STRUCTURES = new ObjectOpenHashSet<>();
    private static @Nullable MultiblockPreviewLevel level;
    private static @Nullable Level sourceLevel;
    private static @Nullable PreviewMesh mesh;
    private static LongList loadedPositions = LongList.of();
    private static long lastPosition;
    private static int lastLayer = -1;
    private static int remainingTicks = -1;
    private static int previewDuration;
    private static @Nullable String terminalFingerprint;

    private MultiblockInWorldPreviewRenderer() {}

    public static void cleanPreview() {
        // Pattern reload listeners can run on the reload executor.
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(MultiblockInWorldPreviewRenderer::cleanPreview);
            return;
        }
        releasePreviewLevel();
        level = null;
        sourceLevel = null;
        loadedPositions = LongList.of();
        remainingTicks = -1;
        lastLayer = -1;
        VISIBLE_STRUCTURES.clear();
        terminalFingerprint = null;
    }

    public static void removePreview(BlockPos pos) {
        if (mesh != null && lastPosition == pos.asLong()) cleanPreview();
    }

    /** Whether this structure is shown at the bound controller in the active client world. */
    public static boolean isPreviewVisible(BlockPos pos, String structure) {
        return sourceLevel == Minecraft.getInstance().level && lastPosition == pos.asLong() &&
                VISIBLE_STRUCTURES.contains(structure);
    }

    /** Toggles a terminal structure using its already resolved positions and block states. */
    public static void togglePreview(BlockPos pos, StructurePreview preview, String structure, int duration) {
        if (sourceLevel != Minecraft.getInstance().level || lastPosition != pos.asLong()) cleanPreview();
        if (!VISIBLE_STRUCTURES.add(structure)) VISIBLE_STRUCTURES.remove(structure);
        showTerminalPreview(pos, preview, duration);
    }

    /** Refreshes an active overlay without resolving the pattern again. */
    public static void updatePreview(BlockPos pos, StructurePreview preview, int duration) {
        if (sourceLevel != Minecraft.getInstance().level || lastPosition != pos.asLong() ||
                preview.fingerprint().equals(terminalFingerprint))
            return;
        VISIBLE_STRUCTURES.removeIf(name -> preview.structures().stream()
                .noneMatch(structure -> structure.name().equals(name)));
        showTerminalPreview(pos, preview, duration);
    }

    private static void showTerminalPreview(BlockPos pos, StructurePreview preview, int duration) {
        if (VISIBLE_STRUCTURES.isEmpty()) {
            cleanPreview();
            return;
        }
        Level source = Minecraft.getInstance().level;
        var previewLevel = new MultiblockPreviewLevel(source);
        var positions = new LongArrayList(preview.cells().size());
        for (var cell : preview.cells()) {
            if (cell.controller() || cell.worldPos() == null) continue;
            for (int index = cell.contributions().size() - 1; index >= 0; index--) {
                var contribution = cell.contributions().get(index);
                if (contribution.action() == CellAction.IGNORE_ANY ||
                        !VISIBLE_STRUCTURES.contains(contribution.structureName()))
                    continue;
                if (!contribution.targetState().isAir()) {
                    previewLevel.addBlock(cell.worldPos(), contribution.renderInfo().createBlockInfo());
                    previewLevel.copyMachineAppearance(source, cell.worldPos(), cell.worldPos());
                    positions.add(cell.worldPos().asLong());
                }
                break;
            }
        }
        beginPreview(source, previewLevel, pos, positions, -1, duration);
        terminalFingerprint = preview.fingerprint();
    }

    /** Shows the controller-oriented plan; repeated use cycles logical pattern layers. */
    public static void showPreview(BlockPos pos, MultiblockControllerMachine controller, int duration) {
        if (!controller.getDefinition().isRenderWorldPreview()) return;
        boolean sameController = sourceLevel == controller.getLevel() && lastPosition == pos.asLong();
        int previousLayer = lastLayer;
        for (int attempt = 0; attempt < MAX_PATTERN_PUBLICATION_ATTEMPTS; attempt++) {
            var planner = new BatchPlanner();
            PatternGenerationGuard.Resolution<BatchPlan> resolution;
            try {
                resolution = PatternGenerationGuard.resolve(StructurePatternRegistry::generation, retry -> planner
                        .resolveControllerPreview(controller, defaultPreviewRequest(controller, planner)));
            } catch (RuntimeException exception) {
                cleanPreview();
                GTCEu.LOGGER.warn("Could not resolve the in-world multiblock preview for {}",
                        controller.getDefinition().getId(), exception);
                return;
            }
            var plan = resolution.value();
            if (!plan.sharedProblems().isEmpty() || plan.mergedCells().isEmpty()) {
                cleanPreview();
                return;
            }
            var heights = new IntAVLTreeSet();
            for (var relative : plan.mergedCells().keySet()) heights.add(relative.getY());
            int[] layers = heights.toIntArray();
            int selectedLayer = sameController ? previousLayer + 1 : -1;
            if (selectedLayer >= layers.length) selectedLayer = -1;
            var previewLevel = new MultiblockPreviewLevel(controller.getLevel());
            var positions = previewBlocks(plan, layers, selectedLayer, previewLevel, controller.getLevel());
            if (StructurePatternRegistry.generation() != resolution.generation()) {
                releasePreviewLevel(previewLevel, positions);
                continue;
            }
            beginPreview(controller.getLevel(), previewLevel, pos, positions, selectedLayer, duration);
            if (StructurePatternRegistry.generation() != resolution.generation()) {
                cleanPreview();
                continue;
            }
            VISIBLE_STRUCTURES.clear();
            for (var structure : plan.executionOrder()) VISIBLE_STRUCTURES.add(structure.structureName());
            terminalFingerprint = null;
            return;
        }
        cleanPreview();
        GTCEu.LOGGER.warn("Pattern generation did not stabilize while publishing the in-world preview for {}",
                controller.getDefinition().getId());
    }

    private static void beginPreview(Level source, MultiblockPreviewLevel previewLevel, BlockPos pos,
                                     LongList positions, int layer, int duration) {
        releasePreviewLevel();
        level = previewLevel;
        sourceLevel = source;
        loadedPositions = positions;
        lastPosition = pos.asLong();
        lastLayer = layer;
        remainingTicks = -1;
        previewDuration = duration;
        previewLevel.setRenderBlocks(positions);
        mesh = new PreviewMesh(previewLevel, positions, null, 0.8f);
    }

    private static LongList previewBlocks(BatchPlan plan, int[] layers, int selectedLayer,
                                          MultiblockPreviewLevel previewLevel, Level source) {
        var positions = new LongArrayList(plan.mergedCells().size());
        for (var merged : plan.mergedCells().values()) {
            if (selectedLayer != -1 && merged.relativePos().getY() != layers[selectedLayer]) continue;
            var representative = renderRepresentative(merged);
            var pos = representative.worldPos();
            if (representative.action() == CellAction.CONTROLLER || pos == null ||
                    representative.blockState().isAir() && !merged.conflict())
                continue;
            var info = representative.blockState().isAir() ?
                    PlannedBlockInfo.from(Blocks.WHITE_STAINED_GLASS.defaultBlockState()) :
                    representative.blockInfo();
            previewLevel.addBlock(pos, info.createBlockInfo());
            previewLevel.copyMachineAppearance(source, pos, pos);
            positions.add(pos.asLong());
        }
        return positions;
    }

    private static PlannedCell renderRepresentative(MergedPlanCell merged) {
        var representative = merged.representative();
        if (!merged.conflict() || !representative.blockState().isAir()) return representative;
        var contributors = merged.contributors();
        for (int index = contributors.size() - 1; index >= 0; index--) {
            var contributor = contributors.get(index);
            if (contributor.action() != CellAction.IGNORE_ANY && !contributor.blockState().isAir()) return contributor;
        }
        return representative;
    }

    private static BatchRequest defaultPreviewRequest(MultiblockControllerMachine controller, BatchPlanner planner) {
        var definition = controller.getDefinition();
        var order = definition.getStructureOrder();
        String main = order.contains(MultiblockControllerMachine.DEFAULT_STRUCTURE) ?
                MultiblockControllerMachine.DEFAULT_STRUCTURE : order.getFirst();
        var selected = new ObjectOpenHashSet<String>();
        selected.add(main);
        selected.addAll(definition.getRequiredStructures(main));
        ObjectList<StructureRequest> structures = order.stream().filter(selected::contains)
                .map(name -> planner.defaultStructureOptions(definition, name))
                .collect(ObjectImmutableList.toList());
        return new BatchRequest(structures, false, true, ObjectList.of());
    }

    public static void onClientTick() {
        if (sourceLevel != null && sourceLevel != Minecraft.getInstance().level) {
            cleanPreview();
        } else if (remainingTicks > 0 && --remainingTicks == 0) {
            cleanPreview();
        }
    }

    public static void renderInWorldPreview(PoseStack poseStack, Camera camera, float partialTicks) {
        if (mesh == null) return;
        try {
            poseStack.pushPose();
            try {
                var eye = camera.getPosition();
                poseStack.translate(-eye.x, -eye.y, -eye.z);
                // The level-stage pose stack is separate from the global camera matrix.
                MODEL_VIEW.set(RenderSystem.getModelViewMatrix()).mul(poseStack.last().pose());
                if (!mesh.compile(MODEL_VIEW)) return;
                if (remainingTicks == -1) remainingTicks = previewDuration;
                mesh.render(MODEL_VIEW, RenderSystem.getProjectionMatrix(), poseStack, partialTicks);
            } finally {
                poseStack.popPose();
            }
        } catch (RuntimeException exception) {
            GTCEu.LOGGER.warn("Could not render the in-world multiblock preview at {}",
                    BlockPos.of(lastPosition), exception);
            cleanPreview();
        }
    }

    private static void releasePreviewLevel() {
        if (mesh != null) {
            mesh.close();
            mesh = null;
        }
        if (level != null) releasePreviewLevel(level, loadedPositions);
    }

    private static void releasePreviewLevel(MultiblockPreviewLevel previewLevel, LongList positions) {
        previewLevel.setBlockFilter(null);
        var pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < positions.size(); i++) {
            var entity = previewLevel.getBlockEntity(pos.set(positions.getLong(i)));
            if (entity != null) entity.setRemoved();
        }
    }
}
