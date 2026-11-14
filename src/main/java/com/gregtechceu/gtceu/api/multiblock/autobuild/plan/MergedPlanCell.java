package com.gregtechceu.gtceu.api.multiblock.autobuild.plan;

import net.minecraft.core.BlockPos;

import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jspecify.annotations.Nullable;

/**
 * All selected structure contributions at one controller-relative position.
 */
public record MergedPlanCell(BlockPos relativePos, @Nullable BlockPos worldPos, ObjectList<PlannedCell> contributors,
                             MergeState mergeState) {

    public boolean conflict() {
        return mergeState == MergeState.CONFLICT;
    }

    /** Returns the final concrete contribution; wildcard cells never mask a real target. */
    public PlannedCell representative() {
        for (int index = contributors.size() - 1; index >= 0; index--) {
            PlannedCell contributor = contributors.get(index);
            if (contributor.action() != CellAction.IGNORE_ANY) return contributor;
        }
        return contributors.getLast();
    }
}
