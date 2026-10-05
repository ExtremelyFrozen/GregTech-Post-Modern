package com.gregtechceu.gtceu.api.multiblock.pattern.model

/** A fragment parameter declaration with an explicit local type. */
sealed interface PatternParameter {
	@JvmRecord
	data class Token(val marker: Unit = Unit) : PatternParameter

	@JvmRecord
	data class Predicate(val marker: Unit = Unit) : PatternParameter

	@JvmRecord
	data class IntegerRange(val minimum: Int, val maximum: Int) : PatternParameter {
		init {
			require(minimum <= maximum) { "Pattern parameter minimum exceeds maximum" }
		}
	}

	@JvmRecord
	data class Direction(val marker: Unit = Unit) : PatternParameter

	@JvmRecord
	data class Fragment(val marker: Unit = Unit) : PatternParameter
}
