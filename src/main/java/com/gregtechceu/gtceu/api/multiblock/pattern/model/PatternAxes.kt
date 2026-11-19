package com.gregtechceu.gtceu.api.multiblock.pattern.model

import org.jspecify.annotations.NullMarked

/** Maps the definition's depth, height and width axes to controller-relative directions. */
@NullMarked
data class PatternAxes(val depth: PatternDirection, val height: PatternDirection, val width: PatternDirection) {

	companion object {
		@JvmStatic
		fun defaults(): PatternAxes = PatternAxes(PatternDirection.FRONT, PatternDirection.UP, PatternDirection.LEFT)
	}
}
