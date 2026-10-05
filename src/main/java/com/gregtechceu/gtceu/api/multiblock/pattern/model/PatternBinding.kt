package com.gregtechceu.gtceu.api.multiblock.pattern.model

import org.jspecify.annotations.NullMarked

/** A value bound to a fragment parameter at a call site. */
@NullMarked
sealed interface PatternBinding {
	@JvmRecord
	data class Token(val value: String) : PatternBinding {
		init {
			require(value.isNotBlank()) { "Token binding must not be blank" }
		}
	}

	@JvmRecord
	data class Predicate(val value: String) : PatternBinding {
		init {
			require(value.isNotBlank()) { "Predicate binding must not be blank" }
		}
	}

	@JvmRecord
	data class IntegerValue(val value: Int) : PatternBinding

	@JvmRecord
	data class Direction(val value: PatternDirection) : PatternBinding

	@JvmRecord
	data class Fragment(val value: String) : PatternBinding {
		init {
			require(value.isNotBlank()) { "Fragment binding must not be blank" }
		}
	}
}
