package com.gregtechceu.gtceu.api.multiblock.autobuild.plan;

import com.gregtechceu.gtceu.api.multiblock.autobuild.AutoBuildProblem;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Complete batch plan consumed by every preview and by the executor.
 */
public record AutoBuildPlan(List<ResolvedStructurePlan> demolitionPlans,
                            List<ResolvedStructurePlan> buildPlans,
                            Map<BlockPos, MergedPlanCell> mergedCells,
                            List<AutoBuildProblem> sharedProblems,
                            long fingerprint) {

    public List<ResolvedStructurePlan> executionOrder() {
        ArrayList<ResolvedStructurePlan> ordered = new ArrayList<>(
                demolitionPlans.size() + buildPlans.size());
        ordered.addAll(demolitionPlans);
        ordered.addAll(buildPlans);
        return List.copyOf(ordered);
    }
}
