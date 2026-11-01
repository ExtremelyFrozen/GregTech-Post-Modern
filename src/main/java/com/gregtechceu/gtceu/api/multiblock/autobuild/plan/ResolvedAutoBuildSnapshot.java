package com.gregtechceu.gtceu.api.multiblock.autobuild.plan;

import com.gregtechceu.gtceu.api.multiblock.autobuild.AutoBuildItemKey;
import com.gregtechceu.gtceu.api.multiblock.autobuild.AutoBuildMaterialSource;

import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectList;

/**
 * One authoritative resolver result, including the exact material snapshots and allocations used by its plan.
 */
public record ResolvedAutoBuildSnapshot(
                                        AutoBuildPlan plan,
                                        ObjectList<AutoBuildMaterialSource.Snapshot> materialSnapshots,
                                        Object2ObjectMap<AutoBuildItemKey, MaterialAllocation> materials) {

    /**
     * Exact demand and source assignment for one material key.
     */
    public record MaterialAllocation(long required, LongList allocatedBySource, long missing,
                                     boolean unlimited) {

    }
}
