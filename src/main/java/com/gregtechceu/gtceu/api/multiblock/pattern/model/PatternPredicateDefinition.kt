package com.gregtechceu.gtceu.api.multiblock.pattern.model

import org.jspecify.annotations.NullMarked

/** A serialized predicate reference and its stable fact aliases. */
@NullMarked
@JvmRecord
data class PatternPredicateDefinition(val type: String, val name: String, val properties: Map<String, Any>, val facts: List<String>) {
	init {
		require(type.isNotBlank()) { "Predicate type must not be blank" }
	}
}
