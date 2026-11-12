package com.gregtechceu.gtceu.api.multiblock.pattern.model.expression

import it.unimi.dsi.fastutil.objects.Object2IntMap

/** A comparison evaluated after expansion, with Span referring to the completed layout bounds. */
data class PatternSizeConstraint(val left: PatternIntegerExpression, val comparison: Comparison, val right: PatternIntegerExpression, val message: String = "") {
	fun matches(parameters: Object2IntMap<String>, spans: IntArray): Boolean {
		val a = left.evaluate(parameters, spans)
		val b = right.evaluate(parameters, spans)
		return compare(a, b)
	}

	private fun compare(a: Int, b: Int): Boolean = when (comparison) {
		Comparison.EQUAL -> a == b
		Comparison.NOT_EQUAL -> a != b
		Comparison.LESS -> a < b
		Comparison.LESS_OR_EQUAL -> a <= b
		Comparison.GREATER -> a > b
		Comparison.GREATER_OR_EQUAL -> a >= b
	}

	enum class Comparison { EQUAL, NOT_EQUAL, LESS, LESS_OR_EQUAL, GREATER, GREATER_OR_EQUAL }
}
