package com.gregtechceu.gtceu.api.multiblock.pattern.model

import org.jspecify.annotations.NullMarked

/** A value bound to a fragment parameter at a call site. */
@NullMarked
sealed interface PatternBinding {
	data class Token(val value: String) : PatternBinding

	data class Predicate(val value: String, val definition: PatternPredicateDefinition? = null) : PatternBinding {
		constructor(definition: PatternPredicateDefinition) : this("", definition)
	}

	data class IntegerValue(val value: Int) : PatternBinding

	data class Direction(val value: PatternDirection) : PatternBinding

	data class Fragment(val value: String) : PatternBinding
}
