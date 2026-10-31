package com.gregtechceu.gtceu.api.multiblock.autobuild.plan;

import com.gregtechceu.gtceu.api.multiblock.autobuild.AutoBuildItemKey;
import com.gregtechceu.gtceu.api.multiblock.autobuild.AutoBuildMaterialSource;

import java.util.List;
import java.util.Map;

/**
 * One authoritative resolver result, including the exact material snapshots and allocations used by its plan.
 */
public record ResolvedAutoBuildSnapshot(
                                        AutoBuildPlan plan,
                                        List<AutoBuildMaterialSource.Snapshot> materialSnapshots,
                                        Map<AutoBuildItemKey, MaterialAllocation> materials) {

    /**
     * Exact demand and source assignment for one material key.
     */
    public record MaterialAllocation(long required, List<Long> allocatedBySource, long missing,
                                     boolean unlimited) {

    }
}
