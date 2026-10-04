package com.gregtechceu.gtceu.api.multiblock.pattern.model

import org.jspecify.annotations.NullMarked

/** A reusable recursive pattern fragment with an explicit parameter scope. */
@NullMarked
@JvmRecord
data class PatternFragment(
    val parameters: Map<String, PatternParameter>,
    val anchors: Map<String, PatternOrigin.Offset>,
    val ports: Map<String, Port>,
    val body: PatternNode,
) {
    @JvmRecord
    data class Port(
        val offset: PatternOrigin.Offset,
        val direction: PatternDirection,
    )
}
