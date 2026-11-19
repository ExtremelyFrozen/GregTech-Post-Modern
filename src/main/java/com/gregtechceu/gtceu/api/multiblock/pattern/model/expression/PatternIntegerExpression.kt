package com.gregtechceu.gtceu.api.multiblock.pattern.model.expression

import com.gregtechceu.gtceu.api.multiblock.pattern.layout.PatternAxis

import it.unimi.dsi.fastutil.objects.Object2IntMap

/** Closed integer arithmetic for source definitions; evaluation never invokes user code. */
sealed interface PatternIntegerExpression {
	fun evaluate(parameters: Object2IntMap<String>, spans: IntArray? = null): Int = when (this) {
		is Constant -> value

		is Parameter -> parameters.getInt(name)

		is Add -> Math.addExact(left.evaluate(parameters, spans), right.evaluate(parameters, spans))

		is Subtract -> Math.subtractExact(left.evaluate(parameters, spans), right.evaluate(parameters, spans))

		is Multiply -> Math.multiplyExact(left.evaluate(parameters, spans), right.evaluate(parameters, spans))

		is FloorDivide -> {
			val numerator = left.evaluate(parameters, spans)
			val denominator = right.evaluate(parameters, spans)
			Math.floorDiv(numerator, denominator)
		}

		is Span -> spans!![axis.ordinal]
	}

	data class Constant(val value: Int) : PatternIntegerExpression
	data class Parameter(val name: String) : PatternIntegerExpression
	data class Add(val left: PatternIntegerExpression, val right: PatternIntegerExpression) : PatternIntegerExpression
	data class Subtract(val left: PatternIntegerExpression, val right: PatternIntegerExpression) : PatternIntegerExpression
	data class Multiply(val left: PatternIntegerExpression, val right: PatternIntegerExpression) : PatternIntegerExpression
	data class FloorDivide(val left: PatternIntegerExpression, val right: PatternIntegerExpression) : PatternIntegerExpression
	data class Span(val axis: PatternAxis) : PatternIntegerExpression
}
