package com.gregtechceu.gtceu.api.multiblock.autobuild.plan;

import com.gregtechceu.gtceu.api.multiblock.autobuild.AutoBuildItemKey;
import com.gregtechceu.gtceu.api.multiblock.pattern.predicate.PatternPredicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jspecify.annotations.Nullable;

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
                          ObjectList<BlockState> stateCandidates, @Nullable Direction requiredDirection,
                          ObjectList<AutoBuildItemKey> materialCandidates, @Nullable AutoBuildItemKey materialKey,
                          ObjectList<String> tierGroups, ObjectList<String> constraintIds) {}
