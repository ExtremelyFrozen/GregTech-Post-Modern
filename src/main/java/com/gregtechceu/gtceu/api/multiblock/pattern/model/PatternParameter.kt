package com.gregtechceu.gtceu.api.multiblock.pattern.model

/** A fragment parameter declaration with an explicit local type. */
sealed interface PatternParameter {
	data class Token(val marker: Unit = Unit) : PatternParameter

	data class Predicate(val marker: Unit = Unit) : PatternParameter

	data class IntegerRange(val minimum: Int, val maximum: Int) : PatternParameter

	data class Direction(val marker: Unit = Unit) : PatternParameter

	data class Fragment(val marker: Unit = Unit) : PatternParameter
}
