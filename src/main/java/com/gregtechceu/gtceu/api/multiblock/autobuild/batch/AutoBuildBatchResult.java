package com.gregtechceu.gtceu.api.multiblock.autobuild.batch;

import java.util.List;

/**
 * Aggregate and per-structure result of one best-effort batch.
 */
public record AutoBuildBatchResult(AutoBuildBatchStatus status, int placed, int removed,
                                   List<AutoBuildStructureResult> structures) {

    public AutoBuildBatchResult {
        if (placed < 0 || removed < 0) {
            throw new IllegalArgumentException("Batch block counts must be non-negative");
        }
        structures = List.copyOf(structures);
        if (structures.isEmpty()) {
            throw new IllegalArgumentException("A batch result requires at least one structure result");
        }
    }

    public static AutoBuildBatchResult of(List<AutoBuildStructureResult> results) {
        if (results.isEmpty()) {
            throw new IllegalArgumentException("A batch result requires at least one structure result");
        }
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
