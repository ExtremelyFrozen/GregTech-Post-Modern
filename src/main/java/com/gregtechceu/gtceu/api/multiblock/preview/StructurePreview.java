package com.gregtechceu.gtceu.api.multiblock.preview;

import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo;
import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialKey;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.BatchPlan;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.BatchResolution;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.CellAction;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.MergedPlanCell;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.PatternCellKey;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.PlannedBlockInfo;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.PlannedCell;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.StructurePlan;
import com.gregtechceu.gtceu.api.sync_system.ContextualFieldCodec;
import com.gregtechceu.gtceu.api.sync_system.FieldCodecs;
import com.gregtechceu.gtceu.api.sync_system.managed.ItemSyncHolder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Comparator;

/**
 * Client-library-neutral projection of one resolved multiblock plan.
 *
 * <p>
 * The wire portion contains no controller, level or live block entity. A locally resolved snapshot may retain a
 * safe {@link PlannedBlockInfo} render description until it is encoded; decoded terminal snapshots reconstruct a
 * descriptor from the exact state, block-entity flag and item form.
 * </p>
 */
public record StructurePreview(ResourceLocation definitionId, String fingerprint,
                               boolean availabilityKnown, ObjectList<Structure> structures,
                               ObjectList<Cell> cells,
                               ObjectList<Material> materials) {

    private static final BlockState AIR_CONFLICT_MARKER_STATE = Blocks.WHITE_STAINED_GLASS.defaultBlockState();

    public static final ContextualFieldCodec<StructurePreview> CODEC = new ContextualFieldCodec<>() {

        @Override
        public Codec<StructurePreview> codec(Context<StructurePreview> context) {
            return FieldCodecs.fromStreamCodec(STREAM_CODEC, ((ItemSyncHolder) context.holder()).getRegistries());
        }

        @Override
        public boolean isImmutable() {
            return true;
        }
    };

    static {
        FieldCodecs.registerContextual(StructurePreview.class, CODEC);
    }
    private static final RenderInfo AIR_CONFLICT_RENDER_INFO = RenderInfo
            .fromPlan(PlannedBlockInfo.from(AIR_CONFLICT_MARKER_STATE));

    /**
     * Registry-aware terminal snapshot codec.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, StructurePreview> STREAM_CODEC = new StreamCodec<>() {

        @Override
        public StructurePreview decode(RegistryFriendlyByteBuf buffer) {
            ResourceLocation definitionId = buffer.readResourceLocation();
            String fingerprint = buffer.readUtf();
            boolean availabilityKnown = buffer.readBoolean();
            ObjectList<Structure> structures = readList(buffer, Structure.STREAM_CODEC);
            ObjectList<Cell> cells = readList(buffer, Cell.STREAM_CODEC);
            ObjectList<Material> materials = readList(buffer, Material.STREAM_CODEC);
            return new StructurePreview(definitionId, fingerprint, availabilityKnown,
                    structures, cells, materials);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, StructurePreview value) {
            buffer.writeResourceLocation(value.definitionId);
            buffer.writeUtf(value.fingerprint);
            buffer.writeBoolean(value.availabilityKnown);
            writeList(buffer, value.structures, Structure.STREAM_CODEC);
            writeList(buffer, value.cells, Cell.STREAM_CODEC);
            writeList(buffer, value.materials, Material.STREAM_CODEC);
        }
    };

    static {
        FieldCodecs.registerStream(StructurePreview.class, STREAM_CODEC);
    }

    /**
     * Projects a canonical XEI plan whose material availability is intentionally unknown.
     */
    public static StructurePreview canonical(ResourceLocation definitionId, BatchPlan plan) {
        return fromPlan(definitionId, plan, null, true);
    }

    /**
     * Projects the exact plan, source snapshots and allocations produced by one server-side resolution.
     */
    public static StructurePreview bound(ResourceLocation definitionId,
                                         BatchResolution resolved) {
        return fromPlan(definitionId, resolved.plan(), resolved, false);
    }

    private static StructurePreview fromPlan(ResourceLocation definitionId, BatchPlan plan,
                                             @Nullable BatchResolution resolved,
                                             boolean retainCandidateGroups) {
        ObjectList<StructurePlan> executionOrder = plan.executionOrder();
        ObjectList<Structure> structures = projectStructures(plan);
        ObjectList<Cell> cells = projectCells(plan, executionOrder);
        ObjectList<Material> materials = projectMaterials(plan, resolved, retainCandidateGroups);
        return new StructurePreview(definitionId, Long.toUnsignedString(plan.fingerprint(), 16),
                resolved != null, structures, cells, materials);
    }

    private static ObjectList<Structure> projectStructures(BatchPlan plan) {
        Object2ObjectLinkedOpenHashMap<String, Mode> structures = new Object2ObjectLinkedOpenHashMap<>();
        ObjectList<StructurePlan> demolitionInDefinitionOrder = new ObjectArrayList<>(plan.demolitionPlans());
        Collections.reverse(demolitionInDefinitionOrder);
        for (StructurePlan structure : demolitionInDefinitionOrder) {
            structures.put(structure.structureName(), Mode.DEMOLISH);
        }
        for (StructurePlan structure : plan.buildPlans()) {
            structures.put(structure.structureName(), Mode.BUILD);
        }
        return structures.entrySet().stream()
                .map(entry -> new Structure(entry.getKey(), entry.getValue()))
                .collect(ObjectImmutableList.toList());
    }

    private static ObjectList<Cell> projectCells(BatchPlan plan,
                                                 ObjectList<StructurePlan> executionOrder) {
        Object2IntMap<String> executionIndex = new Object2IntLinkedOpenHashMap<>();
        for (int index = 0; index < executionOrder.size(); index++) {
            executionIndex.put(executionOrder.get(index).structureName(), index);
        }
        return plan.mergedCells().values().stream()
                .sorted(Comparator.comparingInt((MergedPlanCell merged) -> merged.relativePos().getY())
                        .thenComparingInt(merged -> merged.relativePos().getX())
                        .thenComparingInt(merged -> merged.relativePos().getZ()))
                .map(merged -> {
                    ObjectList<Contribution> contributions = merged.contributors().stream()
                            .sorted(Comparator.comparingInt(contributor -> executionIndex
                                    .getOrDefault(contributor.structureName(), Integer.MAX_VALUE)))
                            .map(Contribution::fromPlan)
                            .collect(ObjectImmutableList.toList());
                    return new Cell(merged.relativePos(), merged.worldPos(), contributions, merged.conflict());
                })
                .collect(ObjectImmutableList.toList());
    }

    private static ObjectList<Material> projectMaterials(BatchPlan plan,
                                                         @Nullable BatchResolution resolved,
                                                         boolean retainCandidateGroups) {
        Object2ObjectLinkedOpenHashMap<ObjectList<MaterialKey>, MaterialRequirement> requirements = collectMaterialRequirements(
                plan,
                retainCandidateGroups);
        Object2ObjectMap<MaterialKey, AllocationCursor> allocations = resolved == null ?
                Object2ObjectMaps.emptyMap() :
                allocationCursors(resolved);
        ObjectList<Material> materials = new ObjectArrayList<>(requirements.size());
        for (var entry : requirements.object2ObjectEntrySet()) {
            ObjectList<MaterialKey> candidates = entry.getKey();
            MaterialRequirement requirement = entry.getValue();
            long missing = 0;
            boolean unlimited = false;
            if (resolved != null) {
                for (var keyRequirement : requirement.keyCounts.object2LongEntrySet()) {
                    @Nullable
                    AllocationCursor cursor = allocations.get(keyRequirement.getKey());
                    if (cursor == null) {
                        throw new IllegalStateException("Resolver omitted a required material allocation");
                    }
                    AllocationPart part = cursor.take(keyRequirement.getLongValue());
                    missing = checkedAdd(missing, part.missing, "missing material count");
                    unlimited |= part.unlimited;
                }
            }
            materials.add(new Material(
                    candidates.stream().map(MaterialKey::prototype).collect(ObjectImmutableList.toList()),
                    requirement.total, missing, unlimited, requirement.structureAmounts()));
        }
        allocations.values().forEach(AllocationCursor::verifyConsumed);
        return new ObjectImmutableList<>(materials);
    }

    private static Object2ObjectLinkedOpenHashMap<ObjectList<MaterialKey>, MaterialRequirement> collectMaterialRequirements(
                                                                                                                            BatchPlan plan,
                                                                                                                            boolean retainCandidateGroups) {
        Object2ObjectLinkedOpenHashMap<ObjectList<MaterialKey>, MaterialRequirement> requirements = new Object2ObjectLinkedOpenHashMap<>();
        for (StructurePlan structure : plan.executionOrder()) {
            Object2LongMap<MaterialKey> remaining = new Object2LongLinkedOpenHashMap<>(structure.materials());
            for (PlannedCell cell : structure.cells()) {
                @Nullable
                MaterialKey selected = cell.materialKey();
                if (cell.action() != CellAction.PLACE || selected == null) continue;
                long count = remaining.getLong(selected);
                if (count <= 0) {
                    throw new IllegalStateException("Resolved structure material totals do not match PLACE cells");
                }
                remaining.put(selected, count - 1);
                ObjectList<MaterialKey> group = materialGroup(cell, selected, retainCandidateGroups);
                requirements.computeIfAbsent(group, ignored -> new MaterialRequirement())
                        .increment(structure.structureName(), selected);
            }
            if (remaining.values().longStream().anyMatch(count -> count != 0)) {
                throw new IllegalStateException("Resolved structure material totals contain unprojected demand");
            }
        }
        return requirements;
    }

    private static ObjectList<MaterialKey> materialGroup(PlannedCell cell, MaterialKey selected,
                                                         boolean retainCandidateGroups) {
        if (retainCandidateGroups && cell.tierGroups().isEmpty() && !cell.materialCandidates().isEmpty()) {
            return cell.materialCandidates();
        }
        return ObjectList.of(selected);
    }

    private static Object2ObjectMap<MaterialKey, AllocationCursor> allocationCursors(BatchResolution resolved) {
        Object2ObjectLinkedOpenHashMap<MaterialKey, AllocationCursor> cursors = new Object2ObjectLinkedOpenHashMap<>();
        resolved.materials().forEach((key, allocation) -> cursors.put(key, new AllocationCursor(allocation)));
        return cursors;
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
                return new Structure(buffer.readUtf(), buffer.readEnum(Mode.class));
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, Structure value) {
                buffer.writeUtf(value.name);
                buffer.writeEnum(value.mode);
            }
        };
    }

    public enum Mode {
        BUILD,
        DEMOLISH
    }

    /**
     * One merged coordinate and all exact structure contributions at that coordinate.
     */
    public record Cell(BlockPos relativePos, @Nullable BlockPos worldPos, ObjectList<Contribution> contributions,
                       boolean conflict) {

        private static final StreamCodec<RegistryFriendlyByteBuf, Cell> STREAM_CODEC = new StreamCodec<>() {

            @Override
            public Cell decode(RegistryFriendlyByteBuf buffer) {
                BlockPos relativePos = buffer.readBlockPos();
                @Nullable
                BlockPos worldPos = buffer.readBoolean() ? buffer.readBlockPos() : null;
                ObjectList<Contribution> contributions = readList(buffer, Contribution.STREAM_CODEC);
                return new Cell(relativePos, worldPos, contributions, buffer.readBoolean());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, Cell value) {
                buffer.writeBlockPos(value.relativePos);
                buffer.writeBoolean(value.worldPos != null);
                if (value.worldPos != null) buffer.writeBlockPos(value.worldPos);
                writeList(buffer, value.contributions, Contribution.STREAM_CODEC);
                buffer.writeBoolean(value.conflict);
            }
        };

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
                    executionRepresentative.requiredDirection, AIR_CONFLICT_RENDER_INFO,
                    executionRepresentative.hatchReplaceable);
        }

        public ObjectList<String> contributorNames() {
            return contributions.stream().map(Contribution::structureName).distinct()
                    .collect(ObjectImmutableList.toList());
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
                               RenderInfo renderInfo, boolean hatchReplaceable) {

        private static final StreamCodec<RegistryFriendlyByteBuf, Contribution> STREAM_CODEC = new StreamCodec<>() {

            @Override
            public Contribution decode(RegistryFriendlyByteBuf buffer) {
                String structureName = buffer.readUtf();
                CellKey cellKey = CellKey.STREAM_CODEC.decode(buffer);
                BlockPos relativePos = buffer.readBlockPos();
                BlockState targetState = buffer.readById(Block.BLOCK_STATE_REGISTRY::byId);
                CellAction action = buffer.readEnum(CellAction.class);
                @Nullable
                Direction requiredDirection = buffer.readBoolean() ?
                        buffer.readEnum(Direction.class) : null;
                RenderInfo renderInfo = RenderInfo.decode(buffer);
                return new Contribution(structureName, cellKey, relativePos, targetState, action,
                        requiredDirection, renderInfo, buffer.readBoolean());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, Contribution value) {
                buffer.writeUtf(value.structureName);
                CellKey.STREAM_CODEC.encode(buffer, value.cellKey);
                buffer.writeBlockPos(value.relativePos);
                buffer.writeById(Block.BLOCK_STATE_REGISTRY::getId, value.targetState);
                buffer.writeEnum(value.action);
                buffer.writeBoolean(value.requiredDirection != null);
                if (value.requiredDirection != null) buffer.writeEnum(value.requiredDirection);
                value.renderInfo.encode(buffer);
                buffer.writeBoolean(value.hatchReplaceable);
            }
        };

        private static Contribution fromPlan(PlannedCell cell) {
            return new Contribution(cell.structureName(), CellKey.fromPlan(cell.cellKey()), cell.relativePos(),
                    cell.blockState(), cell.action(), cell.requiredDirection(), RenderInfo.fromPlan(cell.blockInfo()),
                    cell.hatchReplaceable());
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

        private static CellKey fromPlan(PatternCellKey key) {
            return new CellKey(key.unitIndex(), key.repetition(), key.innerSlice(), key.row(), key.column());
        }
    }

    /**
     * Block render description without a cached block entity. Local snapshots retain the plan initializer; decoded
     * snapshots retain the exact state, block-entity flag and item components.
     */
    public static final class RenderInfo {

        @Getter
        private final BlockState blockState;
        private final boolean hasBlockEntity;
        private final ItemStack itemStack;
        @Nullable
        private final PlannedBlockInfo localPlanInfo;

        private RenderInfo(BlockState blockState, boolean hasBlockEntity, ItemStack itemStack,
                           @Nullable PlannedBlockInfo localPlanInfo) {
            this.blockState = blockState;
            this.hasBlockEntity = hasBlockEntity;
            this.itemStack = itemStack;
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
    public record Material(ObjectList<ItemStack> candidates, long required,
                           long missing, boolean unlimited, ObjectList<StructureAmount> structures) {

        private static final StreamCodec<RegistryFriendlyByteBuf, Material> STREAM_CODEC = new StreamCodec<>() {

            @Override
            public Material decode(RegistryFriendlyByteBuf buffer) {
                int count = buffer.readVarInt();
                ObjectList<ItemStack> candidates = new ObjectArrayList<>(count);
                for (int index = 0; index < count; index++) {
                    candidates.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer));
                }
                long required = buffer.readVarLong();
                long missing = buffer.readVarLong();
                boolean unlimited = buffer.readBoolean();
                ObjectList<StructureAmount> structures = readList(buffer, StructureAmount.STREAM_CODEC);
                return new Material(candidates, required, missing, unlimited, structures);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, Material value) {
                buffer.writeVarInt(value.candidates.size());
                for (ItemStack candidate : value.candidates) {
                    ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, candidate);
                }
                buffer.writeVarLong(value.required);
                buffer.writeVarLong(value.missing);
                buffer.writeBoolean(value.unlimited);
                writeList(buffer, value.structures, StructureAmount.STREAM_CODEC);
            }
        };

        @Override
        public ObjectList<ItemStack> candidates() {
            return candidates.stream().map(ItemStack::copy).collect(ObjectImmutableList.toList());
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
                return new StructureAmount(buffer.readUtf(), buffer.readVarLong());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, StructureAmount value) {
                buffer.writeUtf(value.structureName);
                buffer.writeVarLong(value.required);
            }
        };
    }

    private static <T> ObjectList<T> readList(RegistryFriendlyByteBuf buffer,
                                              StreamCodec<RegistryFriendlyByteBuf, T> codec) {
        int count = buffer.readVarInt();
        ObjectList<T> values = new ObjectArrayList<>(count);
        for (int index = 0; index < count; index++) values.add(codec.decode(buffer));
        return values;
    }

    private static <T> void writeList(RegistryFriendlyByteBuf buffer, ObjectList<T> values,
                                      StreamCodec<RegistryFriendlyByteBuf, T> codec) {
        buffer.writeVarInt(values.size());
        for (T value : values) codec.encode(buffer, value);
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
        private final Object2LongMap<String> structures = new Object2LongLinkedOpenHashMap<>();
        private final Object2LongMap<MaterialKey> keyCounts = new Object2LongLinkedOpenHashMap<>();

        private void increment(String structureName, MaterialKey key) {
            total = checkedAdd(total, 1, "material demand");
            structures.put(structureName, checkedAdd(structures.getLong(structureName), 1,
                    "structure material demand"));
            keyCounts.put(key, checkedAdd(keyCounts.getLong(key), 1, "exact material demand"));
        }

        private ObjectList<StructureAmount> structureAmounts() {
            return structures.object2LongEntrySet().stream()
                    .map(entry -> new StructureAmount(entry.getKey(), entry.getLongValue()))
                    .collect(ObjectImmutableList.toList());
        }
    }

    private static final class AllocationCursor {

        private long remainingRequired;
        private long remainingAllocated;
        private long remainingMissing;
        private final boolean unlimited;

        private AllocationCursor(BatchResolution.MaterialAllocation allocation) {
            remainingRequired = allocation.required();
            remainingAllocated = allocation.allocatedBySource().longStream().sum();
            remainingMissing = allocation.missing();
            unlimited = allocation.unlimited();
        }

        private AllocationPart take(long requested) {
            if (requested < 0 || requested > remainingRequired) {
                throw new IllegalStateException("Preview requested more material allocation than the resolver made");
            }
            remainingRequired -= requested;
            if (unlimited) return new AllocationPart(0, true);
            long allocated = Math.min(requested, remainingAllocated);
            remainingAllocated -= allocated;
            long missing = Math.min(requested - allocated, remainingMissing);
            remainingMissing -= missing;
            return new AllocationPart(missing, false);
        }

        private void verifyConsumed() {
            if (remainingRequired != 0 || remainingMissing != 0 || remainingAllocated != 0) {
                throw new IllegalStateException("Resolver material allocation was not fully projected");
            }
        }
    }

    private record AllocationPart(long missing, boolean unlimited) {}
}
