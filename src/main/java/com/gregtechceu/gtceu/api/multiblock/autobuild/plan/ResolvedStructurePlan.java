package com.gregtechceu.gtceu.api.multiblock.autobuild.plan;

import com.gregtechceu.gtceu.api.multiblock.autobuild.AutoBuildItemKey;
import com.gregtechceu.gtceu.api.multiblock.autobuild.AutoBuildProblem;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildMode;

import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.ObjectList;

/**
 * Complete plan for one named structure.
 */
public record ResolvedStructurePlan(String structureName, AutoBuildMode mode, IntList repetitions,
                                    boolean flipped, ObjectList<PlannedCell> cells,
                                    Object2LongMap<AutoBuildItemKey> materials,
                                    ObjectList<AutoBuildProblem> problems) {

    public boolean valid() {
        return problems.isEmpty();
    }
}
