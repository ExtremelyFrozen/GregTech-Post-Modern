package com.gregtechceu.gtceu.api.multiblock.pattern.model;

import org.jspecify.annotations.NullMarked;

/** A structural constraint evaluated after node matching. */
@NullMarked
public sealed interface PatternConstraint permits PatternConstraint.Count {

    record Count(String fact, Scope scope, int minimum, int maximum, String message) implements PatternConstraint {

        public Count {
            if (fact == null || fact.isBlank() || scope == null) {
                throw new IllegalArgumentException("Count constraints require a fact and scope");
            }
            if (minimum < 0 || maximum < minimum) {
                throw new IllegalArgumentException("Constraint bounds must be finite and ordered");
            }
            message = message == null ? "" : message;
        }
    }

    sealed interface Scope permits Scope.All, Scope.Fragment, Scope.Node, Scope.Repeat {

        record All() implements Scope {}

        record Fragment(String id) implements Scope {

            public Fragment {
                if (id == null || id.isBlank()) throw new IllegalArgumentException("Fragment scope requires an id");
            }
        }

        record Node(String id) implements Scope {

            public Node {
                if (id == null || id.isBlank()) throw new IllegalArgumentException("Node scope requires an id");
            }
        }

        record Repeat(String id) implements Scope {

            public Repeat {
                if (id == null || id.isBlank()) throw new IllegalArgumentException("Repeat scope requires an id");
            }
        }
    }
}
