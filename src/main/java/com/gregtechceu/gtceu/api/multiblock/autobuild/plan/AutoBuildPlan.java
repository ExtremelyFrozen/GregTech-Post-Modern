package com.gregtechceu.gtceu.api.multiblock.autobuild.plan;

import com.gregtechceu.gtceu.api.multiblock.autobuild.AutoBuildProblem;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildMode;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Complete immutable batch plan consumed by every preview and by the executor.
 */
public record AutoBuildPlan(List<ResolvedStructurePlan> demolitionPlans,
                            List<ResolvedStructurePlan> buildPlans,
                            Map<BlockPos, MergedPlanCell> mergedCells,
                            List<AutoBuildProblem> sharedProblems,
                            long fingerprint) {

    public AutoBuildPlan {
        demolitionPlans = List.copyOf(demolitionPlans);
        buildPlans = List.copyOf(buildPlans);
        HashSet<String> structureNames = new HashSet<>();
        for (ResolvedStructurePlan plan : demolitionPlans) {
            if (plan.mode() != AutoBuildMode.DEMOLISH || !structureNames.add(plan.structureName())) {
                throw new IllegalArgumentException("Invalid or duplicate demolition structure plan: " +
                        plan.structureName());
            }
        }
        for (ResolvedStructurePlan plan : buildPlans) {
            if (plan.mode() != AutoBuildMode.BUILD || !structureNames.add(plan.structureName())) {
                throw new IllegalArgumentException("Invalid or duplicate build structure plan: " +
                        plan.structureName());
            }
        }
        mergedCells = Collections.unmodifiableMap(new LinkedHashMap<>(mergedCells));
        sharedProblems = List.copyOf(sharedProblems);
    }

    public List<ResolvedStructurePlan> executionOrder() {
        ArrayList<ResolvedStructurePlan> ordered = new ArrayList<>(
                demolitionPlans.size() + buildPlans.size());
        ordered.addAll(demolitionPlans);
        ordered.addAll(buildPlans);
        return List.copyOf(ordered);
    }
}
