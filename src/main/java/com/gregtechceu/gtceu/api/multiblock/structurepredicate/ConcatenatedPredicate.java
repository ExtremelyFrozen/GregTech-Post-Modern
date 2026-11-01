package com.gregtechceu.gtceu.api.multiblock.structurepredicate;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo;
import com.gregtechceu.gtceu.api.multiblock.MultiblockState;
import com.gregtechceu.gtceu.api.multiblock.pattern.predicate.PatternObservation;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

public record ConcatenatedPredicate(List<StructurePredicate> predicates) implements StructurePredicate {

    @Override
    public ObjectList<BlockState> candidateStates() {
        var states = new ObjectArrayList<BlockState>();
        for (StructurePredicate predicate : predicates) states.addAll(predicate.candidateStates());
        return ObjectLists.unmodifiable(states);
    }

    @Override
    public PatternObservation observeCandidate(BlockState state) {
        for (StructurePredicate predicate : predicates) {
            PatternObservation observation = predicate.observeCandidate(state);
            if (observation.matched()) return observation;
        }
        return PatternObservation.of(false);
    }

    /* using listOf() instead of oneOrMore(), because it makes no sense to allow concat predicate with single element */
    public static final MapCodec<ConcatenatedPredicate> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(StructurePredicate.CODEC.listOf().fieldOf("predicates").forGetter(ConcatenatedPredicate::predicates))
            .apply(instance, ConcatenatedPredicate::new));

    @Override
    public StructurePredicateType<?> type() {
        return StructurePredicateType.CONCATENATED;
    }

    @Override
    public boolean test(MultiblockState multiblockState, boolean mutateCount) {
        boolean matched = false;
        for (StructurePredicate predicate : predicates) {
            matched |= predicate.test(multiblockState, mutateCount);
        }
        return matched;
    }

    @Override
    public @Unmodifiable List<MultiblockBlockInfo> candidates() {
        return predicates.stream().flatMap(p -> p.candidates().stream()).toList();
    }

    @Override
    public @Unmodifiable List<Block> blockCandidates() {
        return predicates.stream().flatMap(p -> p.blockCandidates().stream()).toList();
    }

    @Override
    public @Unmodifiable List<StructurePreviewChoice> previewChoices(MultiblockMachineDefinition definition) {
        return predicates.stream().flatMap(predicate -> predicate.previewChoices(definition).stream()).toList();
    }

    @Override
    public boolean hasAir() {
        return predicates.stream().anyMatch(StructurePredicate::hasAir);
    }

    static ConcatenatedPredicate concat(StructurePredicate a, StructurePredicate b) {
        ImmutableList.Builder<StructurePredicate> builder = ImmutableList.builder();
        if (a instanceof ConcatenatedPredicate(List<StructurePredicate> predicates)) {
            builder.addAll(predicates);
        } else {
            builder.add(a);
        }
        if (b instanceof ConcatenatedPredicate(List<StructurePredicate> predicates)) {
            builder.addAll(predicates);
        } else {
            builder.add(b);
        }
        return new ConcatenatedPredicate(builder.build());
    }
}
