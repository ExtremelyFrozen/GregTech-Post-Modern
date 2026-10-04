package com.gregtechceu.gtceu.api.multiblock.autobuild.plan;

import net.minecraft.core.BlockPos;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * All selected structure contributions at one controller-relative position.
 */
public record MergedPlanCell(BlockPos relativePos, @Nullable BlockPos worldPos, List<PlannedCell> contributors,
                             MergeState mergeState) {

    public MergedPlanCell {
        relativePos = relativePos.immutable();
        worldPos = worldPos == null ? null : worldPos.immutable();
        contributors = List.copyOf(contributors);
        if (contributors.isEmpty()) {
            throw new IllegalArgumentException("A merged plan cell requires at least one contributor");
        }
        for (PlannedCell contributor : contributors) {
            if (!relativePos.equals(contributor.relativePos()) ||
                    !Objects.equals(worldPos, contributor.worldPos())) {
                throw new IllegalArgumentException("Merged plan contributors must share relative and world positions");
            }
        }
    }

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
