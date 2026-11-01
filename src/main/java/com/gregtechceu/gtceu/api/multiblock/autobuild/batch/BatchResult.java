package com.gregtechceu.gtceu.api.multiblock.autobuild.batch;

import it.unimi.dsi.fastutil.objects.ObjectList;

/**
 * Aggregate and per-structure result of one best-effort batch.
 */
public record BatchResult(BatchStatus status, int placed, int removed,
                                   ObjectList<StructureResult> structures) {

    public static BatchResult of(ObjectList<StructureResult> results) {
        int succeeded = 0;
        int placed = 0;
        int removed = 0;
        for (StructureResult result : results) {
            if (result.success()) succeeded++;
            placed = Math.addExact(placed, result.placed());
            removed = Math.addExact(removed, result.removed());
        }
        BatchStatus status = succeeded == results.size() ? BatchStatus.SUCCESS :
                succeeded == 0 ? BatchStatus.FAILED : BatchStatus.PARTIAL_SUCCESS;
        return new BatchResult(status, placed, removed, results);
    }
}
