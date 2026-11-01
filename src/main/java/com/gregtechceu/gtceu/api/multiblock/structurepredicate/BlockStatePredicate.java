package com.gregtechceu.gtceu.api.multiblock.structurepredicate;

import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo;
import com.gregtechceu.gtceu.api.multiblock.MultiblockState;
import com.gregtechceu.gtceu.api.multiblock.pattern.predicate.PatternObservation;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jetbrains.annotations.Unmodifiable;

import static com.gregtechceu.gtceu.api.multiblock.structurepredicate.Util.oneOrMore;

public record BlockStatePredicate(ObjectList<BlockState> blockStates) implements StructurePredicate {

    @Override
    public ObjectList<BlockState> candidateStates() {
        return blockStates;
    }

    @Override
    public PatternObservation observeCandidate(BlockState state) {
        return PatternObservation.of(blockStates.contains(state));
    }

    public static final MapCodec<BlockStatePredicate> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(oneOrMore(BlockState.CODEC).fieldOf("blockStates").forGetter(BlockStatePredicate::blockStates))
            .apply(instance, BlockStatePredicate::new));

    @Override
    public StructurePredicateType<?> type() {
        return StructurePredicateType.BLOCK_STATES;
    }

    @Override
    public ObjectList<MultiblockBlockInfo> candidates() {
        return blockStates.stream().map(MultiblockBlockInfo::new).collect(ObjectImmutableList.toList());
    }

    @Override
    public @Unmodifiable ObjectList<Block> blockCandidates() {
        return blockStates.stream().map(BlockState::getBlock).distinct().collect(ObjectImmutableList.toList());
    }

    @Override
    public boolean test(MultiblockState multiblockState, boolean mutateCount) {
        return blockStates.contains(multiblockState.getBlockState());
    }

    @Override
    public boolean hasAir() {
        return blockStates.contains(Blocks.AIR.defaultBlockState());
    }
}
