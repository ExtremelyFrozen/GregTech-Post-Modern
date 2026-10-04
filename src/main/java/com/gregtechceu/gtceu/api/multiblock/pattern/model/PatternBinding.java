package com.gregtechceu.gtceu.api.multiblock.pattern.model;

import org.jspecify.annotations.NullMarked;

/** A value bound to a fragment parameter at a call site. */
@NullMarked
public sealed interface PatternBinding
                                       permits PatternBinding.Token, PatternBinding.Predicate,
                                       PatternBinding.IntegerValue,
                                       PatternBinding.Direction, PatternBinding.Fragment {

    record Token(String value) implements PatternBinding {

        public Token {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Token binding must not be blank");
            }
        }
    }

    record Predicate(String value) implements PatternBinding {

        public Predicate {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Predicate binding must not be blank");
            }
        }
    }

    record IntegerValue(int value) implements PatternBinding {}

    record Direction(PatternDirection value) implements PatternBinding {

        public Direction {
            if (value == null) {
                throw new IllegalArgumentException("Direction binding must not be null");
            }
        }
    }

    record Fragment(String value) implements PatternBinding {

        public Fragment {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Fragment binding must not be blank");
            }
        }
    }
}
