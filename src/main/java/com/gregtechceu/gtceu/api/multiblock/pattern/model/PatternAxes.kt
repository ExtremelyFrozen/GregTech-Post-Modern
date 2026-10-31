package com.gregtechceu.gtceu.api.multiblock.pattern.model

import org.jspecify.annotations.NullMarked

/** Maps the definition's depth, height and width axes to controller-relative directions. */
@NullMarked
@JvmRecord
data class PatternAxes(val depth: PatternDirection, val height: PatternDirection, val width: PatternDirection) {
	init {
		require(depth.ordinal / 2 != height.ordinal / 2 && depth.ordinal / 2 != width.ordinal / 2 && height.ordinal / 2 != width.ordinal / 2) {
			"Pattern axes must use three different direction pairs"
		}
	}

	companion object {
		@JvmStatic
		fun defaults(): PatternAxes = PatternAxes(PatternDirection.FRONT, PatternDirection.UP, PatternDirection.LEFT)
	}
}
