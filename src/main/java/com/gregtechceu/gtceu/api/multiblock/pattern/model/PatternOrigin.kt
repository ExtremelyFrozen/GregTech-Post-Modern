package com.gregtechceu.gtceu.api.multiblock.pattern.model

import org.jspecify.annotations.NullMarked

/** Defines the controller token and local offset used as the matching origin. */
@NullMarked
@JvmRecord
data class PatternOrigin(
    val token: String,
    val offset: Offset,
) {
    init {
        require(token.isNotBlank()) { "Pattern origin token must not be blank" }
    }

    companion object {
        @JvmStatic
        fun controller(): PatternOrigin = PatternOrigin("controller", Offset(0, 0, 0))
    }

    @JvmRecord
    data class Offset(
        val depth: Int,
        val height: Int,
        val width: Int,
    )
}
