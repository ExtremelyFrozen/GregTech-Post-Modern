package com.gregtechceu.gtceu.api.multiblock.autobuild.plan;

import com.gregtechceu.gtceu.api.multiblock.autobuild.BuildProblem;
import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialKey;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.StructureOperation;

import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.ObjectList;

/**
 * Complete plan for one named structure.
 */
public record StructurePlan(String structureName, StructureOperation mode, IntList repetitions,
                            boolean flipped, ObjectList<PlannedCell> cells,
                            Object2LongMap<MaterialKey> materials,
                            ObjectList<BuildProblem> problems) {

    public boolean valid() {
        return problems.isEmpty();
    }
}
