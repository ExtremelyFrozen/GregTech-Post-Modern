package com.gregtechceu.gtceu.api.multiblock.structurepredicate;

import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo;
import com.gregtechceu.gtceu.api.multiblock.pattern.predicate.PatternObservation;
import com.gregtechceu.gtceu.api.multiblock.pattern.predicate.PatternPredicate;
import com.gregtechceu.gtceu.data.lang.LangHandler;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectList;

import java.util.function.Function;

/**
 * One selectable block family for structure preview generation, together with every nested restriction it carries.
 *
 * @param candidates  ordered block variants; the generator keeps one stable variant throughout a preview page
 * @param constraints nested count constraints applied when this choice is selected
 * @param tooltips    additional candidate descriptions supplied by serialized predicates
 */
public record StructurePreviewChoice(ObjectList<MultiblockBlockInfo> candidates,
                                     ObjectList<StructurePreviewConstraint> constraints,
                                     ObjectList<Component> tooltips,
                                     Function<BlockState, PatternObservation> observer) {

    /** Absent an explicit observer, only the supplied full states can succeed. */
    public StructurePreviewChoice(ObjectList<MultiblockBlockInfo> candidates,
                                  ObjectList<StructurePreviewConstraint> constraints, ObjectList<Component> tooltips) {
        this(candidates, constraints, tooltips, declaredStates(candidates));
    }

    private static Function<BlockState, PatternObservation> declaredStates(ObjectList<MultiblockBlockInfo> candidates) {
        var states = new ObjectLinkedOpenHashSet<BlockState>();
        for (MultiblockBlockInfo candidate : candidates) states.add(candidate.getBlockState());
        return state -> PatternObservation.of(states.contains(state));
    }

    /** The callback must be pure and safe for concurrent read-only calls, just like StructurePredicate observations. */
    public StructurePreviewChoice withObserver(Function<BlockState, PatternObservation> observer) {
        return new StructurePreviewChoice(candidates, constraints, tooltips, observer);
    }

    public PatternObservation observeCandidate(BlockState state) {
        return observer.apply(state);
    }

    /**
     * Creates an unrestricted choice for a leaf predicate.
     */
    public static StructurePreviewChoice unrestricted(ObjectList<MultiblockBlockInfo> candidates) {
        return new StructurePreviewChoice(candidates, ObjectList.of(), ObjectList.of());
    }

    /**
     * Adds an enclosing restriction without discarding constraints owned by nested predicates.
     */
    public StructurePreviewChoice restrictedBy(StructurePreviewConstraint constraint,
                                               ObjectList<Component> additionalTooltips) {
        var combinedConstraints = new ObjectArrayList<StructurePreviewConstraint>(constraints.size() + 1);
        combinedConstraints.addAll(constraints);
        combinedConstraints.add(constraint);
        var combinedTooltips = new ObjectArrayList<Component>(tooltips.size() + additionalTooltips.size());
        combinedTooltips.addAll(tooltips);
        combinedTooltips.addAll(additionalTooltips);
        return new StructurePreviewChoice(candidates, combinedConstraints, combinedTooltips, observer);
    }

    /**
     * Builds the candidate tooltip list, including serialized count rules and the usual predicate hints.
     */
    public ObjectList<Component> getTooltips(PatternPredicate owner) {
        var result = new ObjectArrayList<Component>(tooltips);
        for (StructurePreviewConstraint constraint : constraints) {
            int min = constraint.minCount().orElse(-1);
            int max = constraint.maxCount().orElse(-1);
            if (min == max && max >= 0) {
                result.add(Component.translatable("gtpm.multiblock.pattern.error.limited_exact", min));
            } else if (min >= 0 && max >= 0) {
                result.add(Component.translatable("gtpm.multiblock.pattern.error.limited_within", min, max));
            } else {
                if (min >= 0) {
                    result.add(LangHandler.getFromMultiLang("gtpm.multiblock.pattern.error.limited", 1, min));
                }
                if (max >= 0) {
                    result.add(LangHandler.getFromMultiLang("gtpm.multiblock.pattern.error.limited", 0, max));
                }
            }
        }
        if (owner.isSingle()) {
            result.add(Component.translatable("gtpm.multiblock.pattern.single"));
        }
        if (owner.hasAir()) {
            result.add(Component.translatable("gtpm.multiblock.pattern.replaceable_air"));
        }
        return result;
    }
}
