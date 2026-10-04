package com.gregtechceu.gtceu.api.multiblock.pattern.model;

import org.jspecify.annotations.NullMarked;

/** Maps the definition's depth, height and width axes to controller-relative directions. */
@NullMarked
public record PatternAxes(
                          PatternDirection depth,
                          PatternDirection height,
                          PatternDirection width) {

    public PatternAxes {
        if (depth == null || height == null || width == null) {
            throw new IllegalArgumentException("Pattern axes must be specified");
        }
        if (depth.isHeight() || depth.isWidth() || height.isDepth() || height.isWidth() ||
                width.isDepth() || width.isHeight()) {
            throw new IllegalArgumentException("Pattern axes must use one direction pair per local axis");
        }
        if (depth == height || depth == width || height == width) {
            throw new IllegalArgumentException("Pattern axes must be unique");
        }
    }

    public static PatternAxes defaults() {
        return new PatternAxes(PatternDirection.FRONT, PatternDirection.UP, PatternDirection.LEFT);
    }
}
