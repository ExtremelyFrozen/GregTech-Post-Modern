package com.gregtechceu.gtceu.api.multiblock.pattern.model;

/** A direction in the local coordinate system of a pattern definition. */
public enum PatternDirection {

    FRONT,
    BACK,
    UP,
    DOWN,
    LEFT,
    RIGHT;

    public boolean isDepth() {
        return this == FRONT || this == BACK;
    }

    public boolean isHeight() {
        return this == UP || this == DOWN;
    }

    public boolean isWidth() {
        return this == LEFT || this == RIGHT;
    }
}
