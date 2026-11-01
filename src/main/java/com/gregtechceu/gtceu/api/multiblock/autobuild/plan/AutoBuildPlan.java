package com.gregtechceu.gtceu.api.multiblock.autobuild.plan;

import com.gregtechceu.gtceu.api.multiblock.autobuild.AutoBuildProblem;

import net.minecraft.core.BlockPos;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectList;

/**
 * Complete batch plan consumed by every preview and by the executor.
 */
public record AutoBuildPlan(ObjectList<ResolvedStructurePlan> demolitionPlans,
                            ObjectList<ResolvedStructurePlan> buildPlans,
                            Object2ObjectMap<BlockPos, MergedPlanCell> mergedCells,
                            ObjectList<AutoBuildProblem> sharedProblems,
                            long fingerprint) {

    public ObjectList<ResolvedStructurePlan> executionOrder() {
        ObjectArrayList<ResolvedStructurePlan> ordered = new ObjectArrayList<>(
                demolitionPlans.size() + buildPlans.size());
        ordered.addAll(demolitionPlans);
        ordered.addAll(buildPlans);
        return new ObjectImmutableList<>(ordered);
    }
}
