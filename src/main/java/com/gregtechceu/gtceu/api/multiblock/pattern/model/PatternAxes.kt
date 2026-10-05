package com.gregtechceu.gtceu.api.multiblock.pattern.model

import org.jspecify.annotations.NullMarked

/** Maps the definition's depth, height and width axes to controller-relative directions. */
@NullMarked
@JvmRecord
data class PatternAxes(val depth: PatternDirection, val height: PatternDirection, val width: PatternDirection) {
	init {
		require(
			!depth.isHeight() && !depth.isWidth() &&
				!height.isDepth() && !height.isWidth() &&
				!width.isDepth() && !width.isHeight(),
		) {
			"Pattern axes must use one direction pair per local axis"
		}
		require(depth != height && depth != width && height != width) {
			"Pattern axes must be unique"
		}
	}

	companion object {
		@JvmStatic
		fun defaults(): PatternAxes = PatternAxes(PatternDirection.FRONT, PatternDirection.UP, PatternDirection.LEFT)
	}
}
