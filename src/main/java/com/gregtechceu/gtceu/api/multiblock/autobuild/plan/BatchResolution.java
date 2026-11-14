package com.gregtechceu.gtceu.api.multiblock.autobuild.plan;

import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialKey;

import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;

/**
 * One authoritative resolver result, including the exact material snapshots and allocations used by its plan.
 */
public record BatchResolution(
                              BatchPlan plan,
                              Object2ObjectMap<MaterialKey, MaterialAllocation> materials) {

    /**
     * Exact demand and source assignment for one material key.
     */
    public record MaterialAllocation(long required, LongList allocatedBySource, long missing,
                                     boolean unlimited) {

    }
}
