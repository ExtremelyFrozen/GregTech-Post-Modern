package com.gregtechceu.gtceu.api.multiblock.pattern.model;

import org.jspecify.annotations.NullMarked;

/** A fragment parameter declaration with an explicit local type. */
@NullMarked
public sealed interface PatternParameter
                                         permits PatternParameter.Token, PatternParameter.Predicate,
                                         PatternParameter.IntegerRange,
                                         PatternParameter.Direction, PatternParameter.Fragment {

    record Token() implements PatternParameter {}

    record Predicate() implements PatternParameter {}

    record IntegerRange(int minimum, int maximum) implements PatternParameter {

        public IntegerRange {
            if (minimum > maximum) {
                throw new IllegalArgumentException("Pattern parameter minimum exceeds maximum");
            }
        }
    }

    record Direction() implements PatternParameter {}

    record Fragment() implements PatternParameter {}
}
