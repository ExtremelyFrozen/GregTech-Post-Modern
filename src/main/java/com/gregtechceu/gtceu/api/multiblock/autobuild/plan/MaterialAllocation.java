package com.gregtechceu.gtceu.api.multiblock.autobuild.plan;

import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialKey;
import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialRequest;
import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialSource;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongImmutableList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.Object2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jspecify.annotations.Nullable;

import static com.gregtechceu.gtceu.api.multiblock.autobuild.plan.BatchPlanner.*;
import static com.gregtechceu.gtceu.api.multiblock.autobuild.plan.CandidateExpansion.*;
import static com.gregtechceu.gtceu.api.multiblock.autobuild.plan.PlanAssembly.*;

public final class MaterialAllocation {

    public final ObjectList<Object2LongMap<MaterialKey>> remaining;
    public final ObjectList<Object2LongMap<MaterialKey>> allocated;
    public final LongList sourceFingerprints;
    public final boolean unlimited;
    public final Object2ObjectMap<BlockPos, BlockState> virtualWorld = new Object2ObjectOpenHashMap<>();

    public MaterialAllocation(ObjectList<Object2LongMap<MaterialKey>> remaining,
                              ObjectList<Object2LongMap<MaterialKey>> allocated,
                              LongList sourceFingerprints,
                              boolean unlimited) {
        this.remaining = remaining;
        this.allocated = allocated;
        this.sourceFingerprints = sourceFingerprints;
        this.unlimited = unlimited;
    }

    public static MaterialAllocation create(ObjectList<MaterialRequest.Result> sources,
                                            ObjectCollection<MaterialKey> keys,
                                            ResolutionContext context,
                                            boolean hasBuild) {
        boolean creative = context.player() != null && context.player().isCreative();
        if (!hasBuild || context.unlimitedMaterials() || creative) {
            return new MaterialAllocation(ObjectList.of(), ObjectList.of(), LongList.of(), true);
        }
        ObjectArrayList<Object2LongMap<MaterialKey>> remaining = new ObjectArrayList<>();
        ObjectArrayList<Object2LongMap<MaterialKey>> allocated = new ObjectArrayList<>();
        LongList sourceFingerprints = new LongArrayList();
        for (MaterialRequest.Result binding : sources) {
            MaterialSource source = binding.source();
            MaterialSource.Availability sourceAvailability = source.availability(keys);
            sourceFingerprints
                    .add(hash(binding.providerId().hashCode(), source.planFingerprint(keys, sourceAvailability)));
            remaining.add(new Object2LongLinkedOpenHashMap<>(sourceAvailability.available()));
            allocated.add(new Object2LongLinkedOpenHashMap<>());
        }
        return new MaterialAllocation(remaining, allocated, new LongImmutableList(sourceFingerprints), false);
    }

    @Nullable
    public PlannedCell commitStructure(ObjectList<PlannedCell> cells) {
        if (unlimited) return null;
        ObjectList<Object2LongMap<MaterialKey>> working = copyRemaining();
        ObjectArrayList<Object2LongMap<MaterialKey>> stagedAllocations = new ObjectArrayList<>(
                allocated.size());
        for (int index = 0; index < allocated.size(); index++) {
            stagedAllocations.add(new Object2LongLinkedOpenHashMap<>());
        }
        for (PlannedCell cell : cells) {
            if (cell.action() != CellAction.PLACE || cell.materialKey() == null) continue;
            MaterialKey key = cell.materialKey();
            boolean found = false;
            for (int index = 0; index < working.size(); index++) {
                Object2LongMap<MaterialKey> source = working.get(index);
                long available = source.getLong(key);
                if (available <= 0) continue;
                source.put(key, available - 1);
                stagedAllocations.get(index).mergeLong(key, 1L, Math::addExact);
                found = true;
                break;
            }
            if (!found) return cell;
        }
        for (int index = 0; index < remaining.size(); index++) {
            remaining.get(index).clear();
            remaining.get(index).putAll(working.get(index));
            Object2LongMap<MaterialKey> target = allocated.get(index);
            stagedAllocations.get(index).object2LongEntrySet()
                    .forEach(entry -> target.mergeLong(entry.getKey(), entry.getLongValue(), Math::addExact));
        }
        return null;
    }

    public LongList sourceFingerprints() {
        return sourceFingerprints;
    }

    public Object2ObjectMap<MaterialKey, BatchResolution.MaterialAllocation> resolveAllocations(
                                                                                                ObjectList<StructurePlan> demolitions,
                                                                                                ObjectList<StructurePlan> builds) {
        Object2LongMap<MaterialKey> required = new Object2LongLinkedOpenHashMap<>();
        for (StructurePlan plan : concat(demolitions, builds)) {
            plan.materials().object2LongEntrySet()
                    .forEach(entry -> required.mergeLong(entry.getKey(), entry.getLongValue(), Math::addExact));
        }
        Object2ObjectLinkedOpenHashMap<MaterialKey, BatchResolution.MaterialAllocation> result = new Object2ObjectLinkedOpenHashMap<>();
        required.object2LongEntrySet().forEach(entry -> {
            MaterialKey key = entry.getKey();
            long count = entry.getLongValue();
            LongList bySource = new LongArrayList(allocated.size());
            long assigned = 0;
            for (int index = 0; index < allocated.size(); index++) {
                // Incomplete structures leave stock unallocated; include it when projecting their actual shortage.
                long available = allocated.get(index).getLong(key) + remaining.get(index).getLong(key);
                long value = Math.min(count - assigned, available);
                bySource.add(value);
                assigned = Math.addExact(assigned, value);
            }
            long missing = unlimited ? 0 : count - assigned;
            result.put(key, new BatchResolution.MaterialAllocation(count, bySource, missing,
                    unlimited));
        });
        return Object2ObjectMaps.unmodifiable(result);
    }

    public ObjectList<Object2LongMap<MaterialKey>> copyRemaining() {
        ObjectArrayList<Object2LongMap<MaterialKey>> copy = new ObjectArrayList<>(remaining.size());
        for (Object2LongMap<MaterialKey> source : remaining) {
            copy.add(new Object2LongLinkedOpenHashMap<>(source));
        }
        return copy;
    }

    public ObjectList<UnresolvedCell> applyVirtualWorld(ObjectList<UnresolvedCell> cells) {
        return cells.stream()
                .map(cell -> virtualWorld.containsKey(cell.relativePos()) ?
                        cell.withObservedState(virtualWorld.get(cell.relativePos())) : cell)
                .collect(ObjectImmutableList.toList());
    }

    public void applyPlan(StructurePlan plan) {
        if (!plan.valid()) return;
        for (PlannedCell cell : plan.cells()) {
            switch (cell.action()) {
                case PLACE, UPDATE_DIRECTION, CONTROLLER -> virtualWorld.put(cell.relativePos(), cell.blockState());
                case KEEP -> {
                    BlockState observed = cell.observedState();
                    if (observed == null && cell.worldPos() != null) {
                        throw new IllegalStateException("KEEP plan cell has no observed state: " +
                                cell.cellKey());
                    }
                    virtualWorld.put(cell.relativePos(), observed == null ? cell.blockState() : observed);
                }
                case CLEAR_FOR_AIR, DEMOLISH_CANDIDATE -> virtualWorld.put(cell.relativePos(),
                        Blocks.AIR.defaultBlockState());
                case IGNORE_ANY -> {}
            }
        }
    }
}
