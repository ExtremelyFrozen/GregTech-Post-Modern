package com.gregtechceu.gtceu.api.multiblock.autobuild.plan;

import com.gregtechceu.gtceu.api.multiblock.autobuild.AutoBuildItemKey;
import com.gregtechceu.gtceu.api.multiblock.autobuild.AutoBuildMaterialSource;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One authoritative resolver result, including the exact material snapshots and allocations used by its plan.
 */
public record ResolvedAutoBuildSnapshot(
                                        AutoBuildPlan plan,
                                        List<AutoBuildMaterialSource.Snapshot> materialSnapshots,
                                        Map<AutoBuildItemKey, MaterialAllocation> materials) {

    public ResolvedAutoBuildSnapshot {
        materialSnapshots = List.copyOf(materialSnapshots);
        LinkedHashMap<AutoBuildItemKey, MaterialAllocation> checked = new LinkedHashMap<>();
        for (Map.Entry<AutoBuildItemKey, MaterialAllocation> entry : materials.entrySet()) {
            MaterialAllocation allocation = entry.getValue();
            if (allocation.allocatedBySource().size() != materialSnapshots.size()) {
                throw new IllegalArgumentException("Material allocation source count does not match its snapshots");
            }
            checked.put(entry.getKey(), allocation);
        }
        materials = Collections.unmodifiableMap(checked);
    }

    /**
     * Exact demand and source assignment for one material key.
     */
    public record MaterialAllocation(long required, List<Long> allocatedBySource, long missing,
                                     boolean unlimited) {

        public MaterialAllocation {
            if (required < 0 || missing < 0) {
                throw new IllegalArgumentException("Material counts must be non-negative");
            }
            allocatedBySource = List.copyOf(allocatedBySource);
            long allocated = 0;
            for (long count : allocatedBySource) {
                if (count < 0) {
                    throw new IllegalArgumentException("Allocated material counts must be non-negative");
                }
                allocated = Math.addExact(allocated, count);
            }
            if (unlimited) {
                if (missing != 0) {
                    throw new IllegalArgumentException("Unlimited material allocation cannot have a deficit");
                }
            } else if (Math.addExact(allocated, missing) != required) {
                throw new IllegalArgumentException("Material allocation must account for its exact requirement");
            }
        }
    }
}
