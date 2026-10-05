package com.gregtechceu.gtceu.api.multiblock.pattern.model

import org.jspecify.annotations.NullMarked

/** A structural constraint evaluated after node matching. */
@NullMarked
sealed interface PatternConstraint {
	@JvmRecord
	data class Count(val fact: String, val scope: Scope, val minimum: Int, val maximum: Int, val message: String) : PatternConstraint {
		init {
			require(fact.isNotBlank()) { "Count constraints require a fact" }
			require(minimum >= 0 && maximum >= minimum) {
				"Constraint bounds must be finite and ordered"
			}
		}
	}

	@NullMarked
	sealed interface Scope {
		class All : Scope

		@JvmRecord
		data class Fragment(val id: String) : Scope {
			init {
				require(id.isNotBlank()) { "Fragment scope requires an id" }
			}
		}

		@JvmRecord
		data class Node(val id: String) : Scope {
			init {
				require(id.isNotBlank()) { "Node scope requires an id" }
			}
		}

		@JvmRecord
		data class Repeat(val id: String) : Scope {
			init {
				require(id.isNotBlank()) { "Repeat scope requires an id" }
			}
		}
	}
}
