package com.gregtechceu.gtceu.api.multiblock.pattern.model

import org.jspecify.annotations.NullMarked

/** Defines the controller token and local offset used as the matching origin. */
@NullMarked
data class PatternOrigin(val token: String, val offset: Offset) {
	companion object {
		fun controller(): PatternOrigin = PatternOrigin("controller", Offset(0, 0, 0))
	}

	data class Offset(val depth: Int, val height: Int, val width: Int)
}
