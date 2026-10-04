package com.gregtechceu.gtceu.api.multiblock.autobuild.batch;

import com.gregtechceu.gtceu.api.multiblock.autobuild.AutoBuildProblem;

import java.util.List;

/**
 * Execution outcome for one selected structure.
 */
public record AutoBuildStructureResult(String structureName, AutoBuildMode mode, boolean success,
                                       int placed, int removed, List<AutoBuildProblem> problems) {

    public AutoBuildStructureResult {
        if (structureName.isBlank()) {
            throw new IllegalArgumentException("structureName must not be blank");
        }
        if (placed < 0 || removed < 0) {
            throw new IllegalArgumentException("Structure block counts must be non-negative");
        }
        problems = List.copyOf(problems);
    }
}
