package com.gregtechceu.gtceu.api.multiblock.pattern.model

/** A direction in the local coordinate system of a pattern definition. */
enum class PatternDirection {
    FRONT,
    BACK,
    UP,
    DOWN,
    LEFT,
    RIGHT;

    fun isDepth(): Boolean = this == FRONT || this == BACK

    fun isHeight(): Boolean = this == UP || this == DOWN

    fun isWidth(): Boolean = this == LEFT || this == RIGHT
}
