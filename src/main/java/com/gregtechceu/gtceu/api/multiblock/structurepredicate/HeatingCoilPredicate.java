package com.gregtechceu.gtceu.api.multiblock.structurepredicate;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.block.ICoilType;
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

public enum HeatingCoilPredicate implements StructurePredicate {

    INSTANCE;

    public static final MapCodec<HeatingCoilPredicate> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public StructurePredicateType<?> type() {
        return StructurePredicateType.HEATING_COILS;
    }

    @Override
    public PatternObservation observeCandidate(BlockState state) {
        return PatternPredicates.heatingCoils().observeCandidate(state);
    }

    @Override
    public boolean test(MultiblockState multiblockState, boolean mutateCount) {
        var blockState = multiblockState.getBlockState();
        for (var entry : GTCEuAPI.HEATING_COILS.entrySet()) {
            if (blockState.is(entry.getValue().get())) {
                ICoilType stats = entry.getKey();
                Object currentCoil = multiblockState.getFacts().getOrPut("CoilType", stats);
                if (!currentCoil.equals(stats)) {
                    multiblockState.setError(new PatternStringError("gtpm.multiblock.pattern.error.coils"));
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public @Unmodifiable ObjectList<MultiblockBlockInfo> candidates() {
        return GTCEuAPI.HEATING_COILS.entrySet().stream()
                .sorted(Comparator.comparingInt(value -> value.getKey().getTier()))
                .map(coil -> MultiblockBlockInfo.fromBlockState(coil.getValue().get().defaultBlockState()))
                .collect(ObjectImmutableList.toList());
    }

    @Override
    public @Unmodifiable ObjectList<Block> blockCandidates() {
        return GTCEuAPI.HEATING_COILS.entrySet().stream()
                .sorted(Comparator.comparingInt(value -> value.getKey().getTier()))
                .map(coil -> (Block) coil.getValue().get())
                .collect(ObjectImmutableList.toList());
    }
}
