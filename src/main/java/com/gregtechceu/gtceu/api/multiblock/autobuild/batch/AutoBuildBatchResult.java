package com.gregtechceu.gtceu.api.multiblock.autobuild.batch;

import it.unimi.dsi.fastutil.objects.ObjectList;

/**
 * Aggregate and per-structure result of one best-effort batch.
 */
public record AutoBuildBatchResult(AutoBuildBatchStatus status, int placed, int removed,
                                   ObjectList<AutoBuildStructureResult> structures) {

    public static AutoBuildBatchResult of(ObjectList<AutoBuildStructureResult> results) {
        int succeeded = 0;
        int placed = 0;
        int removed = 0;
        for (AutoBuildStructureResult result : results) {
            if (result.success()) succeeded++;
            placed = Math.addExact(placed, result.placed());
            removed = Math.addExact(removed, result.removed());
        }
        AutoBuildBatchStatus status = succeeded == results.size() ? AutoBuildBatchStatus.SUCCESS :
                succeeded == 0 ? AutoBuildBatchStatus.FAILED : AutoBuildBatchStatus.PARTIAL_SUCCESS;
        return new AutoBuildBatchResult(status, placed, removed, results);
    }
}
