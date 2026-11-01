package com.gregtechceu.gtceu.api.multiblock.structurepredicate;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo;
import com.gregtechceu.gtceu.api.multiblock.MultiblockState;
import com.gregtechceu.gtceu.api.multiblock.pattern.predicate.PatternObservation;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jetbrains.annotations.Unmodifiable;

public record ConcatenatedPredicate(ObjectList<StructurePredicate> predicates) implements StructurePredicate {

    @Override
    public ObjectList<BlockState> candidateStates() {
        var states = new ObjectArrayList<BlockState>();
        for (StructurePredicate predicate : predicates) states.addAll(predicate.candidateStates());
        return states;
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
            .group(StructurePredicate.CODEC.listOf()
                    .<ObjectList<StructurePredicate>>xmap(ObjectArrayList::new, value -> value)
                    .fieldOf("predicates").forGetter(ConcatenatedPredicate::predicates))
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
    public @Unmodifiable ObjectList<MultiblockBlockInfo> candidates() {
        return predicates.stream().flatMap(p -> p.candidates().stream()).collect(ObjectImmutableList.toList());
    }

    @Override
    public @Unmodifiable ObjectList<Block> blockCandidates() {
        return predicates.stream().flatMap(p -> p.blockCandidates().stream()).collect(ObjectImmutableList.toList());
    }

    @Override
    public @Unmodifiable ObjectList<StructurePreviewChoice> previewChoices(MultiblockMachineDefinition definition) {
        return predicates.stream().flatMap(predicate -> predicate.previewChoices(definition).stream())
                .collect(ObjectImmutableList.toList());
    }

    @Override
    public boolean hasAir() {
        return predicates.stream().anyMatch(StructurePredicate::hasAir);
    }

    static ConcatenatedPredicate concat(StructurePredicate a, StructurePredicate b) {
        ObjectList<StructurePredicate> predicates = new ObjectArrayList<>();
        if (a instanceof ConcatenatedPredicate concatenated) {
            predicates.addAll(concatenated.predicates);
        } else {
            predicates.add(a);
        }
        if (b instanceof ConcatenatedPredicate concatenated) {
            predicates.addAll(concatenated.predicates);
        } else {
            predicates.add(b);
        }
        return new ConcatenatedPredicate(predicates);
    }
}
