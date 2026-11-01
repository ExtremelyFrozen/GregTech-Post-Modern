package com.gregtechceu.gtceu.api.multiblock.structurepredicate;

import com.gregtechceu.gtceu.api.multiblock.MultiblockState;
import com.gregtechceu.gtceu.api.multiblock.pattern.predicate.PatternObservation;

import net.minecraft.world.level.block.state.BlockState;

import com.mojang.serialization.MapCodec;

public enum AnyPredicate implements StructurePredicate {

    INSTANCE;

    public static final MapCodec<AnyPredicate> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public StructurePredicateType<?> type() {
        return StructurePredicateType.ANY;
    }

    @Override
    public PatternObservation observeCandidate(BlockState state) {
        return PatternObservation.of(true);
    }

    @Override
    public boolean test(MultiblockState multiblockState, boolean mutateCount) {
        return true;
    }
}
