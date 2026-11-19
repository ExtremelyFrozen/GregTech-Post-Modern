package com.gregtechceu.gtceu.api.multiblock.pattern.model.measurement

import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternDirection
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternOrigin
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternPredicateDefinition

/** Boundary distance along a definition direction, bound to one declared root integer parameter. */
data class PatternMeasurement(val id: String, val parameter: String, val start: PatternOrigin.Offset, val direction: PatternDirection, val minimum: Int, val maximum: Int, val boundary: PatternPredicateDefinition)
