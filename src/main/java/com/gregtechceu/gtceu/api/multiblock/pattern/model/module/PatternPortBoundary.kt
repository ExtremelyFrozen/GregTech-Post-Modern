package com.gregtechceu.gtceu.api.multiblock.pattern.model.module

import com.gregtechceu.gtceu.api.multiblock.pattern.layout.PatternAxis

/** Chooses the first/last cell plane on one axis; the port offset is then added to that plane. */
data class PatternPortBoundary(val axis: PatternAxis, val side: Side) {
	enum class Side { BEFORE, AFTER }
}
