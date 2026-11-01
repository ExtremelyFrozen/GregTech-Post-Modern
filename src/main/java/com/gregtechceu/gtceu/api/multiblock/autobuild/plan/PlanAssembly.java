package com.gregtechceu.gtceu.api.multiblock.autobuild.plan;

import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialKey;

import net.minecraft.core.BlockPos;

import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;

final class PlanAssembly {

    static final long FINGERPRINT_OFFSET = 0xcbf29ce484222325L;
    private static final long FINGERPRINT_PRIME = 0x100000001b3L;

    static Object2ObjectLinkedOpenHashMap<BlockPos, MergedPlanCell> mergeCells(ObjectList<StructurePlan> demolitions,
                                                                               ObjectList<StructurePlan> builds) {
        Object2ObjectLinkedOpenHashMap<BlockPos, ObjectArrayList<PlannedCell>> grouped = new Object2ObjectLinkedOpenHashMap<>();
        for (StructurePlan plan : concat(demolitions, builds)) {
            for (PlannedCell cell : plan.cells()) {
                grouped.computeIfAbsent(cell.relativePos(), ignored -> new ObjectArrayList<>()).add(cell);
            }
        }
        Object2ObjectLinkedOpenHashMap<BlockPos, MergedPlanCell> result = new Object2ObjectLinkedOpenHashMap<>();
        grouped.forEach((pos, contributors) -> {
            ObjectArrayList<PlannedCell> ordered = new ObjectArrayList<>(contributors.size());
            contributors.stream().filter(cell -> cell.action() == CellAction.IGNORE_ANY).forEach(ordered::add);
            contributors.stream().filter(cell -> cell.action() != CellAction.IGNORE_ANY).forEach(ordered::add);
            MergeState state = compatible(ordered) ? MergeState.COMPATIBLE :
                    MergeState.CONFLICT;
            result.put(pos, new MergedPlanCell(pos, ordered.getFirst().worldPos(), ordered, state));
        });
        return result;
    }

    static ObjectList<StructurePlan> concat(ObjectList<StructurePlan> first,
                                            ObjectList<StructurePlan> second) {
        ObjectArrayList<StructurePlan> result = new ObjectArrayList<>(first.size() + second.size());
        result.addAll(first);
        result.addAll(second);
        return result;
    }

    static boolean compatible(ObjectList<PlannedCell> contributors) {
        PlannedCell first = null;
        for (PlannedCell other : contributors) {
            if (other.action() == CellAction.IGNORE_ANY) continue;
            if (first == null) {
                first = other;
                continue;
            }
            boolean firstDemolishes = first.action() == CellAction.DEMOLISH_CANDIDATE;
            boolean otherDemolishes = other.action() == CellAction.DEMOLISH_CANDIDATE;
            if (firstDemolishes != otherDemolishes || !first.blockState().equals(other.blockState())) {
                return false;
            }
        }
        return true;
    }

    static long fingerprint(
                            ObjectList<StructurePlan> demolitions,
                            ObjectList<StructurePlan> builds,
                            Object2ObjectMap<BlockPos, MergedPlanCell> merged,
                            LongList sourceFingerprints,
                            Object2ObjectMap<MaterialKey, BatchResolution.MaterialAllocation> allocations) {
        long value = FINGERPRINT_OFFSET;
        for (StructurePlan structure : concat(demolitions, builds)) {
            value = hash(value, structure.structureName().hashCode());
            value = hash(value, structure.mode().ordinal());
            value = hash(value, structure.repetitions().hashCode());
            value = hash(value, structure.flipped() ? 1 : 0);
            for (PlannedCell cell : structure.cells()) {
                value = hash(value, cell.relativePos().asLong());
                value = hash(value, cell.blockState().hashCode());
                value = hash(value, cell.observedState() == null ? 0 : cell.observedState().hashCode());
                value = hash(value, cell.action().ordinal());
                value = hash(value, cell.stateCandidates().hashCode());
                value = hash(value, cell.requiredDirection() == null ? -1 : cell.requiredDirection().ordinal());
                value = hash(value, cell.materialKey() == null ? 0 : cell.materialKey().hashCode());
                value = hash(value, cell.tierGroups().hashCode());
            }
        }
        for (MergedPlanCell cell : merged.values()) {
            value = hash(value, cell.mergeState().ordinal());
        }
        for (long sourceFingerprint : sourceFingerprints) {
            value = hash(value, sourceFingerprint);
        }
        for (var entry : allocations.object2ObjectEntrySet()) {
            value = hash(value, entry.getKey().hashCode());
            BatchResolution.MaterialAllocation allocation = entry.getValue();
            value = hash(value, allocation.required());
            for (long allocated : allocation.allocatedBySource()) {
                value = hash(value, allocated);
            }
            value = hash(value, allocation.missing());
            value = hash(value, allocation.unlimited() ? 1 : 0);
        }
        return value;
    }

    static long hash(long current, long value) {
        return (current ^ value) * FINGERPRINT_PRIME;
    }
}
