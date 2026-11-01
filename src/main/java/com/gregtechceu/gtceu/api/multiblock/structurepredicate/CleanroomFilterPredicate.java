package com.gregtechceu.gtceu.api.multiblock.structurepredicate;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.block.IFilterType;
import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo;
import com.gregtechceu.gtceu.api.multiblock.MultiblockState;
import com.gregtechceu.gtceu.api.multiblock.error.PatternStringError;
import com.gregtechceu.gtceu.api.multiblock.pattern.predicate.PatternObservation;
import com.gregtechceu.gtceu.api.multiblock.pattern.predicate.PatternPredicates;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Comparator;

public enum CleanroomFilterPredicate implements StructurePredicate {

    INSTANCE;

    public static final MapCodec<CleanroomFilterPredicate> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public StructurePredicateType<?> type() {
        return StructurePredicateType.CLEANROOM_FILTERS;
    }

    @Override
    public PatternObservation observeCandidate(BlockState state) {
        return PatternPredicates.cleanroomFilters().observeCandidate(state);
    }

    @Override
    public boolean test(MultiblockState multiblockState, boolean mutateCount) {
        var blockState = multiblockState.getBlockState();
        for (var entry : GTCEuAPI.CLEANROOM_FILTERS.entrySet()) {
            if (blockState.is(entry.getValue().get())) {
                IFilterType stats = entry.getKey();
                Object currentFilter = multiblockState.getFacts().getOrPut("FilterType", stats);
                if (!currentFilter.equals(stats)) {
                    multiblockState.setError(new PatternStringError("gtpm.multiblock.pattern.error.filters"));
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public @Unmodifiable ObjectList<MultiblockBlockInfo> candidates() {
        return GTCEuAPI.CLEANROOM_FILTERS.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().getSerializedName()))
                .map(entry -> MultiblockBlockInfo.fromBlockState(entry.getValue().get().defaultBlockState()))
                .collect(ObjectImmutableList.toList());
    }

    @Override
    public @Unmodifiable ObjectList<Block> blockCandidates() {
        return GTCEuAPI.CLEANROOM_FILTERS.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().getSerializedName()))
                .map(entry -> entry.getValue().get())
                .collect(ObjectImmutableList.toList());
    }
}
