package com.gregtechceu.gtceu.api.multiblock.pattern.predicate;

import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternPredicateDefinition;

import org.jspecify.annotations.NullMarked;

import java.util.function.Function;

/** Registry entry that turns a stable predicate definition into the runtime predicate. */
@NullMarked
public record PredicateType(Function<PatternPredicateDefinition, PatternPredicate> compiler) {

    public PredicateType {
        if (compiler == null) throw new IllegalArgumentException("Predicate compiler must not be null");
    }

    public PatternPredicate compile(PatternPredicateDefinition definition) {
        return compiler.apply(definition);
    }
}
