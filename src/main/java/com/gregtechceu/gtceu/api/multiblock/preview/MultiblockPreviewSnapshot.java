package com.gregtechceu.gtceu.api.multiblock.preview;

import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo;
import com.gregtechceu.gtceu.api.multiblock.autobuild.AutoBuildItemKey;
import com.gregtechceu.gtceu.api.multiblock.autobuild.AutoBuildMaterialSource;
import com.gregtechceu.gtceu.api.multiblock.autobuild.AutoBuildProblem;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.AutoBuildPlan;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.CellAction;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.MergedPlanCell;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.PatternCellKey;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.PlannedBlockInfo;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.PlannedCell;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.ResolvedAutoBuildSnapshot;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.ResolvedStructurePlan;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Immutable, client-library-neutral projection of one resolved multiblock plan.
 *
 * <p>
 * The wire portion contains no controller, level or live block entity. A locally resolved snapshot may retain a
 * safe {@link PlannedBlockInfo} render description until it is encoded; decoded terminal snapshots reconstruct a
 * descriptor from the exact state, block-entity flag and item form.
 * </p>
 */
public record MultiblockPreviewSnapshot(ResourceLocation definitionId, String fingerprint,
                                        boolean availabilityKnown, List<Structure> structures, List<Cell> cells,
                                        List<Material> materials, List<Diagnostic> diagnostics) {

    public static final int MAX_STRUCTURES = 128;
    public static final int MAX_CELLS = 65_536;
    public static final int MAX_MATERIALS = 4_096;
    public static final int MAX_DIAGNOSTICS = 4_096;
    public static final int MAX_CONTRIBUTORS = 128;
    public static final int MAX_CANDIDATES = 4_096;
    private static final int MAX_NAME_LENGTH = 256;
    private static final int MAX_FINGERPRINT_LENGTH = 128;
    private static final int MAX_DIAGNOSTIC_CODE_LENGTH = 128;
    private static final BlockState AIR_CONFLICT_MARKER_STATE = Blocks.WHITE_STAINED_GLASS.defaultBlockState();
    private static final RenderInfo AIR_CONFLICT_RENDER_INFO = RenderInfo
            .fromPlan(PlannedBlockInfo.from(AIR_CONFLICT_MARKER_STATE));

    /**
     * Registry-aware and explicitly bounded terminal snapshot codec.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, MultiblockPreviewSnapshot> STREAM_CODEC = new StreamCodec<>() {

        @Override
        public MultiblockPreviewSnapshot decode(RegistryFriendlyByteBuf buffer) {
            ResourceLocation definitionId = buffer.readResourceLocation();
            String fingerprint = buffer.readUtf(MAX_FINGERPRINT_LENGTH);
            boolean availabilityKnown = buffer.readBoolean();
            List<Structure> structures = readList(buffer, MAX_STRUCTURES, Structure.STREAM_CODEC);
            List<Cell> cells = readList(buffer, MAX_CELLS, Cell.STREAM_CODEC);
            List<Material> materials = readList(buffer, MAX_MATERIALS, Material.STREAM_CODEC);
            List<Diagnostic> diagnostics = readList(buffer, MAX_DIAGNOSTICS, Diagnostic.STREAM_CODEC);
            return new MultiblockPreviewSnapshot(definitionId, fingerprint, availabilityKnown,
                    structures, cells, materials, diagnostics);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, MultiblockPreviewSnapshot value) {
            buffer.writeResourceLocation(value.definitionId);
            buffer.writeUtf(value.fingerprint, MAX_FINGERPRINT_LENGTH);
            buffer.writeBoolean(value.availabilityKnown);
            writeList(buffer, value.structures, MAX_STRUCTURES, Structure.STREAM_CODEC);
            writeList(buffer, value.cells, MAX_CELLS, Cell.STREAM_CODEC);
            writeList(buffer, value.materials, MAX_MATERIALS, Material.STREAM_CODEC);
            writeList(buffer, value.diagnostics, MAX_DIAGNOSTICS, Diagnostic.STREAM_CODEC);
        }
    };

    public MultiblockPreviewSnapshot {
        requireStringLength(fingerprint, MAX_FINGERPRINT_LENGTH, "fingerprint");
        structures = boundedCopy(structures, MAX_STRUCTURES, "structures");
        cells = boundedCopy(cells, MAX_CELLS, "cells");
        materials = boundedCopy(materials, MAX_MATERIALS, "materials");
        diagnostics = boundedCopy(diagnostics, MAX_DIAGNOSTICS, "diagnostics");

        Set<String> names = new HashSet<>();
        for (Structure structure : structures) {
            if (!names.add(structure.name)) {
                throw new IllegalArgumentException("Duplicate preview structure: " + structure.name);
            }
        }
        for (Cell cell : cells) {
            for (Contribution contribution : cell.contributions) {
                if (!names.contains(contribution.structureName)) {
                    throw new IllegalArgumentException(
                            "Preview cell references an unknown structure: " + contribution.structureName);
                }
            }
        }
        for (Material material : materials) {
            for (StructureAmount amount : material.structures) {
                if (!names.contains(amount.structureName)) {
                    throw new IllegalArgumentException(
                            "Preview material references an unknown structure: " + amount.structureName);
                }
            }
        }
        if (availabilityKnown) {
            for (Material material : materials) {
                long accounted = checkedAdd(material.meAllocated, material.playerAllocated,
                        "allocated material count");
                accounted = checkedAdd(accounted, material.missing, "accounted material count");
                if (material.unlimited) {
                    if (accounted != 0) {
                        throw new IllegalArgumentException(
                                "Unlimited preview material must not be assigned to a bounded source");
                    }
                } else if (accounted != material.required) {
                    throw new IllegalArgumentException(
                            "Preview material allocation does not exactly account for its requirement");
                }
            }
        }
    }

    /**
     * Projects a canonical XEI plan whose material availability is intentionally unknown.
     */
    public static MultiblockPreviewSnapshot canonical(ResourceLocation definitionId, AutoBuildPlan plan) {
        return fromPlan(definitionId, plan, null, true);
    }

    /**
     * Projects the exact plan, source snapshots and allocations produced by one server-side resolution.
     */
    public static MultiblockPreviewSnapshot bound(ResourceLocation definitionId,
                                                  ResolvedAutoBuildSnapshot resolved) {
        return fromPlan(definitionId, resolved.plan(), resolved, false);
    }

    private static MultiblockPreviewSnapshot fromPlan(ResourceLocation definitionId, AutoBuildPlan plan,
                                                      @Nullable ResolvedAutoBuildSnapshot resolved,
                                                      boolean retainCandidateGroups) {
        List<ResolvedStructurePlan> executionOrder = plan.executionOrder();
        List<Structure> structures = structureSnapshots(plan);
        List<Cell> cells = cellSnapshots(plan, executionOrder);
        List<Material> materials = materialSnapshots(plan, resolved, retainCandidateGroups);
        List<Diagnostic> diagnostics = diagnosticSnapshots(plan);
        return new MultiblockPreviewSnapshot(definitionId, Long.toUnsignedString(plan.fingerprint(), 16),
                resolved != null, structures, cells, materials, diagnostics);
    }

    private static List<Structure> structureSnapshots(AutoBuildPlan plan) {
        LinkedHashMap<String, Mode> structures = new LinkedHashMap<>();
        List<ResolvedStructurePlan> demolitionInDefinitionOrder = new ArrayList<>(plan.demolitionPlans());
        Collections.reverse(demolitionInDefinitionOrder);
        for (ResolvedStructurePlan structure : demolitionInDefinitionOrder) {
            structures.put(structure.structureName(), Mode.DEMOLISH);
        }
        for (ResolvedStructurePlan structure : plan.buildPlans()) {
            structures.put(structure.structureName(), Mode.BUILD);
        }
        return structures.entrySet().stream()
                .map(entry -> new Structure(entry.getKey(), entry.getValue()))
                .toList();
    }

    private static List<Cell> cellSnapshots(AutoBuildPlan plan, List<ResolvedStructurePlan> executionOrder) {
        Map<String, Integer> executionIndex = new LinkedHashMap<>();
        for (int index = 0; index < executionOrder.size(); index++) {
            executionIndex.put(executionOrder.get(index).structureName(), index);
        }
        return plan.mergedCells().values().stream()
                .sorted(Comparator.comparingInt((MergedPlanCell merged) -> merged.relativePos().getY())
                        .thenComparingInt(merged -> merged.relativePos().getX())
                        .thenComparingInt(merged -> merged.relativePos().getZ()))
                .map(merged -> {
                    List<Contribution> contributions = merged.contributors().stream()
                            .sorted(Comparator.comparingInt(contributor -> executionIndex
                                    .getOrDefault(contributor.structureName(), Integer.MAX_VALUE)))
                            .map(Contribution::fromPlan)
                            .toList();
                    return new Cell(merged.relativePos(), merged.worldPos(), contributions, merged.conflict());
                })
                .toList();
    }

    private static List<Material> materialSnapshots(AutoBuildPlan plan,
                                                    @Nullable ResolvedAutoBuildSnapshot resolved,
                                                    boolean retainCandidateGroups) {
        LinkedHashMap<List<AutoBuildItemKey>, MaterialRequirement> requirements = collectMaterialRequirements(plan,
                retainCandidateGroups);
        Map<AutoBuildItemKey, AllocationCursor> allocations = resolved == null ? Map.of() :
                allocationCursors(resolved);
        List<Material> materials = new ArrayList<>(requirements.size());
        for (Map.Entry<List<AutoBuildItemKey>, MaterialRequirement> entry : requirements.entrySet()) {
            List<AutoBuildItemKey> candidates = entry.getKey();
            MaterialRequirement requirement = entry.getValue();
            long meAvailable = resolved == null ? 0 :
                    availability(candidates, resolved.materialSnapshots(), AutoBuildMaterialSource.SourceKind.ME);
            long playerAvailable = resolved == null ? 0 : availability(
                    candidates, resolved.materialSnapshots(), AutoBuildMaterialSource.SourceKind.PLAYER);
            long meAllocated = 0;
            long playerAllocated = 0;
            long missing = 0;
            boolean unlimited = false;
            if (resolved != null) {
                for (Map.Entry<AutoBuildItemKey, Long> keyRequirement : requirement.keyCounts.entrySet()) {
                    @Nullable
                    AllocationCursor cursor = allocations.get(keyRequirement.getKey());
                    if (cursor == null) {
                        throw new IllegalStateException("Resolver omitted a required material allocation");
                    }
                    AllocationPart part = cursor.take(keyRequirement.getValue(), resolved.materialSnapshots());
                    meAllocated = checkedAdd(meAllocated, part.meAllocated, "ME allocation");
                    playerAllocated = checkedAdd(playerAllocated, part.playerAllocated, "player allocation");
                    missing = checkedAdd(missing, part.missing, "missing material count");
                    unlimited |= part.unlimited;
                }
            }
            materials.add(new Material(candidates.stream().map(AutoBuildItemKey::prototype).toList(),
                    requirement.total, meAvailable, meAllocated, playerAvailable, playerAllocated, missing,
                    unlimited, requirement.structureAmounts()));
        }
        allocations.values().forEach(AllocationCursor::verifyConsumed);
        return List.copyOf(materials);
    }

    private static LinkedHashMap<List<AutoBuildItemKey>, MaterialRequirement> collectMaterialRequirements(
                                                                                                          AutoBuildPlan plan,
                                                                                                          boolean retainCandidateGroups) {
        LinkedHashMap<List<AutoBuildItemKey>, MaterialRequirement> requirements = new LinkedHashMap<>();
        for (ResolvedStructurePlan structure : plan.executionOrder()) {
            LinkedHashMap<AutoBuildItemKey, Long> remaining = new LinkedHashMap<>(structure.materials());
            for (PlannedCell cell : structure.cells()) {
                @Nullable
                AutoBuildItemKey selected = cell.materialKey();
                if (cell.action() != CellAction.PLACE || selected == null) continue;
                long count = remaining.getOrDefault(selected, 0L);
                if (count <= 0) {
                    throw new IllegalStateException("Resolved structure material totals do not match PLACE cells");
                }
                remaining.put(selected, count - 1);
                List<AutoBuildItemKey> group = materialGroup(cell, selected, retainCandidateGroups);
                requirements.computeIfAbsent(group, ignored -> new MaterialRequirement())
                        .increment(structure.structureName(), selected);
            }
            if (remaining.values().stream().anyMatch(count -> count != 0)) {
                throw new IllegalStateException("Resolved structure material totals contain unprojected demand");
            }
        }
        return requirements;
    }

    private static List<AutoBuildItemKey> materialGroup(PlannedCell cell, AutoBuildItemKey selected,
                                                        boolean retainCandidateGroups) {
        if (retainCandidateGroups && cell.tierGroups().isEmpty() && !cell.materialCandidates().isEmpty()) {
            return cell.materialCandidates();
        }
        return List.of(selected);
    }

    private static Map<AutoBuildItemKey, AllocationCursor> allocationCursors(ResolvedAutoBuildSnapshot resolved) {
        LinkedHashMap<AutoBuildItemKey, AllocationCursor> cursors = new LinkedHashMap<>();
        resolved.materials().forEach((key, allocation) -> cursors.put(key, new AllocationCursor(allocation)));
        return cursors;
    }

    private static long availability(List<AutoBuildItemKey> keys,
                                     List<AutoBuildMaterialSource.Snapshot> snapshots,
                                     AutoBuildMaterialSource.SourceKind kind) {
        long total = 0;
        for (AutoBuildMaterialSource.Snapshot snapshot : snapshots) {
            if (snapshot.kind() != kind || snapshot.problem() != null) continue;
            for (AutoBuildItemKey key : keys) {
                total = checkedAdd(total, snapshot.available(key), kind + " availability");
            }
        }
        return total;
    }

    private static List<Diagnostic> diagnosticSnapshots(AutoBuildPlan plan) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        for (AutoBuildProblem problem : plan.sharedProblems()) {
            diagnostics.add(new Diagnostic(problem.type().name(), null, relativeProblemPos(problem, plan)));
        }
        for (ResolvedStructurePlan structure : plan.executionOrder()) {
            for (AutoBuildProblem problem : structure.problems()) {
                diagnostics.add(new Diagnostic(problem.type().name(), structure.structureName(),
                        relativeProblemPos(problem, plan)));
            }
        }
        return diagnostics;
    }

    @Nullable
    private static BlockPos relativeProblemPos(AutoBuildProblem problem, AutoBuildPlan plan) {
        if (problem.pos() == null) return null;
        for (MergedPlanCell cell : plan.mergedCells().values()) {
            if (problem.pos().equals(cell.worldPos()) || problem.pos().equals(cell.relativePos())) {
                return cell.relativePos();
            }
        }
        return null;
    }

    /**
     * Returns inclusive controller-relative vertical bounds, or {@code [0, 0]} for an empty plan.
     */
    public int[] verticalBounds() {
        if (cells.isEmpty()) return new int[] { 0, 0 };
        int minimum = Integer.MAX_VALUE;
        int maximum = Integer.MIN_VALUE;
        for (Cell cell : cells) {
            minimum = Math.min(minimum, cell.relativePos.getY());
            maximum = Math.max(maximum, cell.relativePos.getY());
        }
        return new int[] { minimum, maximum };
    }

    /**
     * One selected structure and its mode.
     */
    public record Structure(String name, Mode mode) {

        private static final StreamCodec<RegistryFriendlyByteBuf, Structure> STREAM_CODEC = new StreamCodec<>() {

            @Override
            public Structure decode(RegistryFriendlyByteBuf buffer) {
                return new Structure(buffer.readUtf(MAX_NAME_LENGTH), buffer.readEnum(Mode.class));
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, Structure value) {
                buffer.writeUtf(value.name, MAX_NAME_LENGTH);
                buffer.writeEnum(value.mode);
            }
        };

        public Structure {
            if (name.isBlank()) throw new IllegalArgumentException("Preview structure name must not be blank");
            requireStringLength(name, MAX_NAME_LENGTH, "structure name");
        }
    }

    public enum Mode {
        BUILD,
        DEMOLISH
    }

    /**
     * One merged coordinate and all exact structure contributions at that coordinate.
     */
    public record Cell(BlockPos relativePos, @Nullable BlockPos worldPos, List<Contribution> contributions,
                       boolean conflict) {

        private static final StreamCodec<RegistryFriendlyByteBuf, Cell> STREAM_CODEC = new StreamCodec<>() {

            @Override
            public Cell decode(RegistryFriendlyByteBuf buffer) {
                BlockPos relativePos = buffer.readBlockPos();
                @Nullable
                BlockPos worldPos = buffer.readBoolean() ? buffer.readBlockPos() : null;
                List<Contribution> contributions = readList(buffer, MAX_CONTRIBUTORS, Contribution.STREAM_CODEC);
                return new Cell(relativePos, worldPos, contributions, buffer.readBoolean());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, Cell value) {
                buffer.writeBlockPos(value.relativePos);
                buffer.writeBoolean(value.worldPos != null);
                if (value.worldPos != null) buffer.writeBlockPos(value.worldPos);
                writeList(buffer, value.contributions, MAX_CONTRIBUTORS, Contribution.STREAM_CODEC);
                buffer.writeBoolean(value.conflict);
            }
        };

        public Cell {
            relativePos = relativePos.immutable();
            worldPos = worldPos == null ? null : worldPos.immutable();
            contributions = boundedCopy(contributions, MAX_CONTRIBUTORS, "cell contributions");
            if (contributions.isEmpty()) {
                throw new IllegalArgumentException("Preview cell must have at least one contribution");
            }
            for (Contribution contribution : contributions) {
                if (!relativePos.equals(contribution.relativePos)) {
                    throw new IllegalArgumentException("Preview contribution uses a different relative position");
                }
            }
        }

        public BlockState state() {
            return representative().targetState;
        }

        /**
         * Returns the state used to build preview geometry. An AIR target remains the execution result, but a
         * conflicting earlier non-air contribution is kept visible so the conflict can still be selected and
         * inspected.
         */
        public BlockState renderState() {
            return renderRepresentative().targetState;
        }

        public RenderInfo renderInfo() {
            return renderRepresentative().renderInfo;
        }

        /** Uses the final concrete target so wildcards never mask the state left by execution order. */
        public Contribution representative() {
            for (int index = contributions.size() - 1; index >= 0; index--) {
                Contribution contribution = contributions.get(index);
                if (contribution.action != CellAction.IGNORE_ANY) return contribution;
            }
            return contributions.getLast();
        }

        /** Uses a concrete contribution, or a synthetic glass marker when every conflicting target is AIR. */
        public Contribution renderRepresentative() {
            Contribution executionRepresentative = representative();
            if (!conflict || !executionRepresentative.targetState.isAir()) {
                return executionRepresentative;
            }
            for (int index = contributions.size() - 1; index >= 0; index--) {
                Contribution contribution = contributions.get(index);
                if (contribution.action != CellAction.IGNORE_ANY && !contribution.targetState.isAir()) {
                    return contribution;
                }
            }
            return new Contribution(executionRepresentative.structureName, executionRepresentative.cellKey,
                    executionRepresentative.relativePos, AIR_CONFLICT_MARKER_STATE, executionRepresentative.action,
                    executionRepresentative.requiredDirection, AIR_CONFLICT_RENDER_INFO);
        }

        public List<String> contributorNames() {
            return contributions.stream().map(Contribution::structureName).distinct().toList();
        }

        public boolean controller() {
            return contributions.stream().anyMatch(contribution -> contribution.action == CellAction.CONTROLLER);
        }

        public boolean demolitionTarget() {
            return contributions.stream()
                    .anyMatch(contribution -> contribution.action == CellAction.DEMOLISH_CANDIDATE);
        }
    }

    /**
     * Exact contribution retained for conflict tooltips and stable error focus.
     */
    public record Contribution(String structureName, CellKey cellKey, BlockPos relativePos,
                               BlockState targetState, CellAction action, @Nullable Direction requiredDirection,
                               RenderInfo renderInfo) {

        private static final StreamCodec<RegistryFriendlyByteBuf, Contribution> STREAM_CODEC = new StreamCodec<>() {

            @Override
            public Contribution decode(RegistryFriendlyByteBuf buffer) {
                String structureName = buffer.readUtf(MAX_NAME_LENGTH);
                CellKey cellKey = CellKey.STREAM_CODEC.decode(buffer);
                BlockPos relativePos = buffer.readBlockPos();
                BlockState targetState = buffer.readById(Block.BLOCK_STATE_REGISTRY::byId);
                CellAction action = buffer.readEnum(CellAction.class);
                @Nullable
                Direction requiredDirection = buffer.readBoolean() ?
                        buffer.readEnum(Direction.class) : null;
                RenderInfo renderInfo = RenderInfo.decode(buffer);
                return new Contribution(structureName, cellKey, relativePos, targetState, action,
                        requiredDirection, renderInfo);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, Contribution value) {
                buffer.writeUtf(value.structureName, MAX_NAME_LENGTH);
                CellKey.STREAM_CODEC.encode(buffer, value.cellKey);
                buffer.writeBlockPos(value.relativePos);
                buffer.writeById(Block.BLOCK_STATE_REGISTRY::getId, value.targetState);
                buffer.writeEnum(value.action);
                buffer.writeBoolean(value.requiredDirection != null);
                if (value.requiredDirection != null) buffer.writeEnum(value.requiredDirection);
                value.renderInfo.encode(buffer);
            }
        };

        public Contribution {
            if (structureName.isBlank()) {
                throw new IllegalArgumentException("Preview contribution structure must not be blank");
            }
            requireStringLength(structureName, MAX_NAME_LENGTH, "contribution structure");
            relativePos = relativePos.immutable();
            if (!targetState.equals(renderInfo.blockState())) {
                throw new IllegalArgumentException("Preview render state does not match its contribution target");
            }
        }

        private static Contribution fromPlan(PlannedCell cell) {
            return new Contribution(cell.structureName(), CellKey.fromPlan(cell.cellKey()), cell.relativePos(),
                    cell.blockState(), cell.action(), cell.requiredDirection(), RenderInfo.fromPlan(cell.blockInfo()));
        }
    }

    /**
     * Stable expanded pattern address used by preview diagnostics.
     */
    public record CellKey(int unitIndex, int repetition, int innerSlice, int row, int column) {

        private static final StreamCodec<RegistryFriendlyByteBuf, CellKey> STREAM_CODEC = new StreamCodec<>() {

            @Override
            public CellKey decode(RegistryFriendlyByteBuf buffer) {
                return new CellKey(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                        buffer.readVarInt(), buffer.readVarInt());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, CellKey value) {
                buffer.writeVarInt(value.unitIndex);
                buffer.writeVarInt(value.repetition);
                buffer.writeVarInt(value.innerSlice);
                buffer.writeVarInt(value.row);
                buffer.writeVarInt(value.column);
            }
        };

        public CellKey {
            if (unitIndex < 0 || repetition < 0 || innerSlice < 0 || row < 0 || column < 0) {
                throw new IllegalArgumentException("Preview cell indexes must be non-negative");
            }
        }

        private static CellKey fromPlan(PatternCellKey key) {
            return new CellKey(key.unitIndex(), key.repetition(), key.innerSlice(), key.row(), key.column());
        }
    }

    /**
     * Block render description without a cached block entity. Local snapshots retain the plan initializer; decoded
     * snapshots retain the exact state, block-entity flag and item components.
     */
    public static final class RenderInfo {

        private final BlockState blockState;
        private final boolean hasBlockEntity;
        private final ItemStack itemStack;
        @Nullable
        private final PlannedBlockInfo localPlanInfo;

        private RenderInfo(BlockState blockState, boolean hasBlockEntity, ItemStack itemStack,
                           @Nullable PlannedBlockInfo localPlanInfo) {
            this.blockState = blockState;
            this.hasBlockEntity = hasBlockEntity;
            this.itemStack = itemStack.copy();
            this.localPlanInfo = localPlanInfo;
        }

        private static RenderInfo fromPlan(PlannedBlockInfo info) {
            return new RenderInfo(info.blockState(), info.hasBlockEntity(), info.itemStack(), info);
        }

        private static RenderInfo decode(RegistryFriendlyByteBuf buffer) {
            BlockState blockState = buffer.readById(Block.BLOCK_STATE_REGISTRY::byId);
            boolean hasBlockEntity = buffer.readBoolean();
            ItemStack itemStack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
            return new RenderInfo(blockState, hasBlockEntity, itemStack, null);
        }

        private void encode(RegistryFriendlyByteBuf buffer) {
            buffer.writeById(Block.BLOCK_STATE_REGISTRY::getId, blockState);
            buffer.writeBoolean(hasBlockEntity);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, itemStack);
        }

        public BlockState blockState() {
            return blockState;
        }

        public ItemStack itemStack() {
            return itemStack.copy();
        }

        public MultiblockBlockInfo createBlockInfo() {
            if (localPlanInfo != null) return localPlanInfo.createBlockInfo();
            return new MultiblockBlockInfo(blockState, hasBlockEntity, itemStack.copy(), null);
        }

        @Override
        public boolean equals(Object object) {
            return object == this || object instanceof RenderInfo other &&
                    blockState.equals(other.blockState) && hasBlockEntity == other.hasBlockEntity &&
                    ItemStack.isSameItemSameComponents(itemStack, other.itemStack);
        }

        @Override
        public int hashCode() {
            int result = 31 * blockState.hashCode() + Boolean.hashCode(hasBlockEntity);
            return 31 * result + ItemStack.hashItemAndComponents(itemStack);
        }
    }

    /**
     * One complete, never-truncated material row.
     */
    public record Material(List<ItemStack> candidates, long required,
                           long meAvailable, long meAllocated,
                           long playerAvailable, long playerAllocated,
                           long missing, boolean unlimited, List<StructureAmount> structures) {

        private static final StreamCodec<RegistryFriendlyByteBuf, Material> STREAM_CODEC = new StreamCodec<>() {

            @Override
            public Material decode(RegistryFriendlyByteBuf buffer) {
                int count = readBoundedCount(buffer, MAX_CANDIDATES, "material candidates");
                List<ItemStack> candidates = new ArrayList<>(count);
                for (int index = 0; index < count; index++) {
                    candidates.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer));
                }
                long required = buffer.readVarLong();
                long meAvailable = buffer.readVarLong();
                long meAllocated = buffer.readVarLong();
                long playerAvailable = buffer.readVarLong();
                long playerAllocated = buffer.readVarLong();
                long missing = buffer.readVarLong();
                boolean unlimited = buffer.readBoolean();
                List<StructureAmount> structures = readList(buffer, MAX_STRUCTURES, StructureAmount.STREAM_CODEC);
                return new Material(candidates, required, meAvailable, meAllocated, playerAvailable,
                        playerAllocated, missing, unlimited, structures);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, Material value) {
                writeCount(buffer, value.candidates.size(), MAX_CANDIDATES, "material candidates");
                for (ItemStack candidate : value.candidates) {
                    ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, candidate);
                }
                buffer.writeVarLong(value.required);
                buffer.writeVarLong(value.meAvailable);
                buffer.writeVarLong(value.meAllocated);
                buffer.writeVarLong(value.playerAvailable);
                buffer.writeVarLong(value.playerAllocated);
                buffer.writeVarLong(value.missing);
                buffer.writeBoolean(value.unlimited);
                writeList(buffer, value.structures, MAX_STRUCTURES, StructureAmount.STREAM_CODEC);
            }
        };

        public Material {
            if (candidates.isEmpty()) {
                throw new IllegalArgumentException("Preview material must have at least one candidate");
            }
            if (candidates.size() > MAX_CANDIDATES) {
                throw new IllegalArgumentException("Too many preview material candidates: " + candidates.size());
            }
            List<ItemStack> copies = new ArrayList<>(candidates.size());
            for (ItemStack candidate : candidates) {
                if (candidate.isEmpty()) {
                    throw new IllegalArgumentException("Preview material candidate must not be empty");
                }
                ItemStack copy = candidate.copy();
                copy.setCount(1);
                copies.add(copy);
            }
            candidates = List.copyOf(copies);
            requireNonNegative(required, "required");
            requireNonNegative(meAvailable, "meAvailable");
            requireNonNegative(meAllocated, "meAllocated");
            requireNonNegative(playerAvailable, "playerAvailable");
            requireNonNegative(playerAllocated, "playerAllocated");
            requireNonNegative(missing, "missing");
            if (meAllocated > meAvailable || playerAllocated > playerAvailable || missing > required) {
                throw new IllegalArgumentException("Preview material allocation exceeds availability or demand");
            }
            if (unlimited && missing != 0) {
                throw new IllegalArgumentException("Unlimited material demand cannot be missing");
            }
            structures = boundedCopy(structures, MAX_STRUCTURES, "material structure amounts");
            long structureTotal = 0;
            Set<String> structureNames = new HashSet<>();
            for (StructureAmount structure : structures) {
                if (!structureNames.add(structure.structureName)) {
                    throw new IllegalArgumentException(
                            "Duplicate material structure amount: " + structure.structureName);
                }
                structureTotal = checkedAdd(structureTotal, structure.required, "structure material total");
            }
            if (structureTotal != required) {
                throw new IllegalArgumentException(
                        "Material structure amounts do not add up to the aggregate requirement");
            }
        }

        @Override
        public List<ItemStack> candidates() {
            return candidates.stream().map(ItemStack::copy).toList();
        }

        public ItemStack displayStack() {
            ItemStack display = candidates.getFirst().copy();
            display.setCount((int) Math.min(required, Integer.MAX_VALUE));
            return display;
        }
    }

    /**
     * Per-structure contribution to one aggregate material row.
     */
    public record StructureAmount(String structureName, long required) {

        private static final StreamCodec<RegistryFriendlyByteBuf, StructureAmount> STREAM_CODEC = new StreamCodec<>() {

            @Override
            public StructureAmount decode(RegistryFriendlyByteBuf buffer) {
                return new StructureAmount(buffer.readUtf(MAX_NAME_LENGTH), buffer.readVarLong());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, StructureAmount value) {
                buffer.writeUtf(value.structureName, MAX_NAME_LENGTH);
                buffer.writeVarLong(value.required);
            }
        };

        public StructureAmount {
            if (structureName.isBlank()) {
                throw new IllegalArgumentException("Material structure name must not be blank");
            }
            requireStringLength(structureName, MAX_NAME_LENGTH, "material structure name");
            if (required <= 0) {
                throw new IllegalArgumentException("Material structure requirement must be positive");
            }
        }
    }

    /**
     * Stable diagnostic code plus optional structure/cell focus target.
     */
    public record Diagnostic(String code, @Nullable String structureName, @Nullable BlockPos relativePos) {

        private static final StreamCodec<RegistryFriendlyByteBuf, Diagnostic> STREAM_CODEC = new StreamCodec<>() {

            @Override
            public Diagnostic decode(RegistryFriendlyByteBuf buffer) {
                String code = buffer.readUtf(MAX_DIAGNOSTIC_CODE_LENGTH);
                @Nullable
                String structure = buffer.readBoolean() ? buffer.readUtf(MAX_NAME_LENGTH) : null;
                @Nullable
                BlockPos pos = buffer.readBoolean() ? buffer.readBlockPos() : null;
                return new Diagnostic(code, structure, pos);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, Diagnostic value) {
                buffer.writeUtf(value.code, MAX_DIAGNOSTIC_CODE_LENGTH);
                buffer.writeBoolean(value.structureName != null);
                if (value.structureName != null) buffer.writeUtf(value.structureName, MAX_NAME_LENGTH);
                buffer.writeBoolean(value.relativePos != null);
                if (value.relativePos != null) buffer.writeBlockPos(value.relativePos);
            }
        };

        public Diagnostic {
            if (code.isBlank()) throw new IllegalArgumentException("Preview diagnostic code must not be blank");
            requireStringLength(code, MAX_DIAGNOSTIC_CODE_LENGTH, "diagnostic code");
            if (structureName != null) {
                if (structureName.isBlank()) {
                    throw new IllegalArgumentException("Diagnostic structure name must not be blank");
                }
                requireStringLength(structureName, MAX_NAME_LENGTH, "diagnostic structure name");
            }
            relativePos = relativePos == null ? null : relativePos.immutable();
        }
    }

    private static <T> List<T> readList(RegistryFriendlyByteBuf buffer, int maximum,
                                        StreamCodec<RegistryFriendlyByteBuf, T> codec) {
        int count = readBoundedCount(buffer, maximum, "preview list");
        List<T> values = new ArrayList<>(count);
        for (int index = 0; index < count; index++) values.add(codec.decode(buffer));
        return values;
    }

    private static <T> void writeList(RegistryFriendlyByteBuf buffer, List<T> values, int maximum,
                                      StreamCodec<RegistryFriendlyByteBuf, T> codec) {
        writeCount(buffer, values.size(), maximum, "preview list");
        for (T value : values) codec.encode(buffer, value);
    }

    private static int readBoundedCount(RegistryFriendlyByteBuf buffer, int maximum, String label) {
        int count = buffer.readVarInt();
        if (count < 0 || count > maximum) {
            throw new IllegalArgumentException(label + " count is outside [0, " + maximum + "]: " + count);
        }
        return count;
    }

    private static void writeCount(RegistryFriendlyByteBuf buffer, int count, int maximum, String label) {
        if (count < 0 || count > maximum) {
            throw new IllegalArgumentException(label + " count is outside [0, " + maximum + "]: " + count);
        }
        buffer.writeVarInt(count);
    }

    private static <T> List<T> boundedCopy(List<T> values, int maximum, String label) {
        if (values.size() > maximum) {
            throw new IllegalArgumentException("Too many " + label + ": " + values.size());
        }
        return List.copyOf(values);
    }

    private static void requireStringLength(String value, int maximum, String label) {
        if (value.length() > maximum) {
            throw new IllegalArgumentException(label + " is longer than " + maximum + " characters");
        }
    }

    private static void requireNonNegative(long value, String label) {
        if (value < 0) throw new IllegalArgumentException(label + " must not be negative");
    }

    private static long checkedAdd(long left, long right, String label) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("COUNT_OVERFLOW while adding " + label, exception);
        }
    }

    private static final class MaterialRequirement {

        private long total;
        private final LinkedHashMap<String, Long> structures = new LinkedHashMap<>();
        private final LinkedHashMap<AutoBuildItemKey, Long> keyCounts = new LinkedHashMap<>();

        private void increment(String structureName, AutoBuildItemKey key) {
            total = checkedAdd(total, 1, "material demand");
            structures.put(structureName, checkedAdd(structures.getOrDefault(structureName, 0L), 1,
                    "structure material demand"));
            keyCounts.put(key, checkedAdd(keyCounts.getOrDefault(key, 0L), 1, "exact material demand"));
        }

        private List<StructureAmount> structureAmounts() {
            return structures.entrySet().stream()
                    .map(entry -> new StructureAmount(entry.getKey(), entry.getValue()))
                    .toList();
        }
    }

    private static final class AllocationCursor {

        private long remainingRequired;
        private final long[] remainingBySource;
        private long remainingMissing;
        private final boolean unlimited;

        private AllocationCursor(ResolvedAutoBuildSnapshot.MaterialAllocation allocation) {
            remainingRequired = allocation.required();
            remainingBySource = new long[allocation.allocatedBySource().size()];
            for (int index = 0; index < remainingBySource.length; index++) {
                remainingBySource[index] = allocation.allocatedBySource().get(index);
            }
            remainingMissing = allocation.missing();
            unlimited = allocation.unlimited();
        }

        private AllocationPart take(long requested, List<AutoBuildMaterialSource.Snapshot> snapshots) {
            if (requested < 0 || requested > remainingRequired) {
                throw new IllegalStateException("Preview requested more material allocation than the resolver made");
            }
            remainingRequired -= requested;
            if (unlimited) return new AllocationPart(0, 0, 0, true);
            long remaining = requested;
            long me = 0;
            long player = 0;
            for (int index = 0; index < remainingBySource.length && remaining > 0; index++) {
                long allocated = Math.min(remaining, remainingBySource[index]);
                remainingBySource[index] -= allocated;
                remaining -= allocated;
                AutoBuildMaterialSource.SourceKind kind = snapshots.get(index).kind();
                if (kind == AutoBuildMaterialSource.SourceKind.ME) {
                    me = checkedAdd(me, allocated, "ME allocation");
                } else if (kind == AutoBuildMaterialSource.SourceKind.PLAYER) {
                    player = checkedAdd(player, allocated, "player allocation");
                }
            }
            long missing = Math.min(remaining, remainingMissing);
            remainingMissing -= missing;
            remaining -= missing;
            if (remaining != 0) {
                throw new IllegalStateException("Resolver allocation contains an unsupported material source kind");
            }
            return new AllocationPart(me, player, missing, false);
        }

        private void verifyConsumed() {
            if (remainingRequired != 0 || remainingMissing != 0) {
                throw new IllegalStateException("Resolver material allocation was not fully projected");
            }
            for (long remaining : remainingBySource) {
                if (remaining != 0) {
                    throw new IllegalStateException("Resolver source allocation was not fully projected");
                }
            }
        }
    }

    private record AllocationPart(long meAllocated, long playerAllocated, long missing, boolean unlimited) {}
}
