package com.gregtechceu.gtceu.api.multiblock.pattern.model

import org.jspecify.annotations.NullMarked

/** A structural constraint evaluated after node matching. */
@NullMarked
sealed interface PatternConstraint {
	data class Count(val fact: String, val scope: Scope, val minimum: Int, val maximum: Int, val message: String) : PatternConstraint

	@NullMarked
	sealed interface Scope {
		class All : Scope

		data class Fragment(val id: String) : Scope

		data class Node(val id: String) : Scope

		data class Repeat(val id: String) : Scope
	}
}
