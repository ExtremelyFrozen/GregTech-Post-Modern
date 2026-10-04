package com.gregtechceu.gtceu.api.multiblock.autobuild.plan;

import com.gregtechceu.gtceu.api.multiblock.autobuild.AutoBuildItemKey;
import com.gregtechceu.gtceu.api.multiblock.pattern.predicate.PatternPredicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * One exact, resolved session cell shared by preview, material accounting and execution.
 *
 * <p>
 * The predicate and block initializer belong to the owning machine definition, so this record must not be used as
 * a network payload or retained in a global cache. Client snapshots must project it to immutable render data.
 * </p>
 */
public record PlannedCell(String structureName, PatternCellKey cellKey, BlockPos relativePos,
                          @Nullable BlockPos worldPos, BlockState blockState, @Nullable BlockState observedState,
                          PlannedBlockInfo blockInfo,
                          CellAction action, PatternPredicate predicate,
                          List<BlockState> stateCandidates, @Nullable Direction requiredDirection,
                          List<AutoBuildItemKey> materialCandidates, @Nullable AutoBuildItemKey materialKey,
                          List<ResourceLocation> tierGroups, List<String> constraintIds) {

    public PlannedCell {
        if (structureName.isBlank()) {
            throw new IllegalArgumentException("structureName must not be blank");
        }
        relativePos = relativePos.immutable();
        worldPos = worldPos == null ? null : worldPos.immutable();
        stateCandidates = List.copyOf(stateCandidates);
        materialCandidates = List.copyOf(materialCandidates);
        tierGroups = List.copyOf(tierGroups);
        constraintIds = List.copyOf(constraintIds);
    }
}
