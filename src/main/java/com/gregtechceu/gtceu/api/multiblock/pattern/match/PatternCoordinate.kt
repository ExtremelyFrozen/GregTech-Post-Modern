package com.gregtechceu.gtceu.api.multiblock.pattern.match

/** Immutable local coordinate in depth, height, width order. */
@JvmRecord
data class PatternCoordinate(
    val depth: Int,
    val height: Int,
    val width: Int,
)
