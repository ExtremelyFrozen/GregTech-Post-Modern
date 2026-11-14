package com.gregtechceu.gtceu.api.multiblock.pattern.layout

import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternDirection

/** A dimension of the definition, before the controller-relative axes are applied. */
enum class PatternAxis {
	DEPTH,
	HEIGHT,
	WIDTH,
	;

	companion object {
		@JvmStatic
		fun of(direction: PatternDirection): PatternAxis = when {
			direction.isDepth() -> DEPTH
			direction.isHeight() -> HEIGHT
			else -> WIDTH
		}
	}
}
