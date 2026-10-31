package com.gregtechceu.gtceu.api.multiblock.autobuild.plan;

import com.gregtechceu.gtceu.api.multiblock.autobuild.AutoBuildItemKey;
import com.gregtechceu.gtceu.api.multiblock.autobuild.AutoBuildProblem;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildMode;

import java.util.List;
import java.util.Map;

/**
 * Complete plan for one named structure.
 */
public record ResolvedStructurePlan(String structureName, AutoBuildMode mode, List<Integer> repetitions,
                                    boolean flipped, List<PlannedCell> cells,
                                    Map<AutoBuildItemKey, Long> materials,
                                    List<AutoBuildProblem> problems) {

    public boolean valid() {
        return problems.isEmpty();
    }
}
