package com.gregtechceu.gtceu.api.multiblock.autobuild.plan;

import com.gregtechceu.gtceu.api.multiblock.autobuild.AutoBuildItemKey;
import com.gregtechceu.gtceu.api.multiblock.autobuild.AutoBuildProblem;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildMode;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Complete immutable plan for one named structure.
 */
public record ResolvedStructurePlan(String structureName, AutoBuildMode mode, List<Integer> repetitions,
                                    boolean flipped, List<PlannedCell> cells,
                                    Map<AutoBuildItemKey, Long> materials,
                                    List<AutoBuildProblem> problems) {

    public ResolvedStructurePlan {
        if (structureName.isBlank()) {
            throw new IllegalArgumentException("structureName must not be blank");
        }
        repetitions = List.copyOf(repetitions);
        cells = List.copyOf(cells);
        for (PlannedCell cell : cells) {
            if (!structureName.equals(cell.structureName())) {
                throw new IllegalArgumentException("Resolved cell belongs to a different structure: " +
                        cell.structureName());
            }
        }
        LinkedHashMap<AutoBuildItemKey, Long> checkedMaterials = new LinkedHashMap<>();
        materials.forEach((key, count) -> {
            if (count < 0) {
                throw new IllegalArgumentException("Resolved material counts must be non-negative");
            }
            checkedMaterials.put(key, count);
        });
        materials = Collections.unmodifiableMap(checkedMaterials);
        problems = List.copyOf(problems);
    }

    public boolean valid() {
        return problems.isEmpty();
    }
}
