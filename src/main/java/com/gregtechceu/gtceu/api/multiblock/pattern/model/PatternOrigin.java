package com.gregtechceu.gtceu.api.multiblock.pattern.model;

import org.jspecify.annotations.NullMarked;

/** Defines the controller token and local offset used as the matching origin. */
@NullMarked
public record PatternOrigin(String token, Offset offset) {

    public PatternOrigin {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Pattern origin token must not be blank");
        }
        offset = offset == null ? new Offset(0, 0, 0) : offset;
    }

    public static PatternOrigin controller() {
        return new PatternOrigin("controller", new Offset(0, 0, 0));
    }

    public record Offset(int depth, int height, int width) {}
}
