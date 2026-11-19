package com.gregtechceu.gtceu.api.multiblock.pattern.layout

import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternDirection
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternOrigin

/** Signed orthogonal transform between a fragment's local and host port frames. */
data class PatternPortTransform(val depthDirection: PatternDirection, val heightDirection: PatternDirection, val widthDirection: PatternDirection) {
	fun direction(local: PatternDirection): PatternDirection {
		val axis = when {
			local.isDepth() -> depthDirection
			local.isHeight() -> heightDirection
			else -> widthDirection
		}
		val sign = if (local.ordinal % 2 == 0) 1 else -1
		return if (sign > 0) axis else opposite(axis)
	}

	fun point(value: PatternOrigin.Offset): PatternOrigin.Offset {
		val source = intArrayOf(value.depth, value.height, value.width)
		val target = IntArray(3)
		val mapped = arrayOf(depthDirection, heightDirection, widthDirection)
		for (axis in 0..2) {
			val direction = mapped[axis]
			val targetAxis = if (direction.isDepth()) {
				0
			} else if (direction.isHeight()) {
				1
			} else {
				2
			}
			target[targetAxis] = if (direction.ordinal % 2 == 0) source[axis] else Math.negateExact(source[axis])
		}
		return PatternOrigin.Offset(target[0], target[1], target[2])
	}

	private fun opposite(direction: PatternDirection): PatternDirection = PatternDirection.entries[direction.ordinal xor 1]

	companion object {
		val IDENTITY = PatternPortTransform(PatternDirection.FRONT, PatternDirection.UP, PatternDirection.LEFT)

		fun between(input: PatternDirection, inputUp: PatternDirection, output: PatternDirection, outputUp: PatternDirection): PatternPortTransform {
			fun vector(direction: PatternDirection): IntArray = IntArray(3).also {
				it[
					if (direction.isDepth()) {
						0
					} else if (direction.isHeight()) {
						1
					} else {
						2
					},
				] = if (direction.ordinal % 2 == 0) 1 else -1
			}
			fun cross(a: IntArray, b: IntArray): IntArray = intArrayOf(a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])
			val fromForward = vector(input)
			val fromUp = vector(inputUp)
			val fromSide = cross(fromForward, fromUp)
			val toForward = vector(PatternDirection.entries[output.ordinal xor 1])
			val toUp = vector(outputUp)
			val toSide = cross(toForward, toUp)
			fun mapped(axis: Int): PatternDirection {
				val result = IntArray(3) { target -> fromForward[axis] * toForward[target] + fromUp[axis] * toUp[target] + fromSide[axis] * toSide[target] }
				val target = result.indexOfFirst { it != 0 }
				return PatternDirection.entries[target * 2 + if (result[target] > 0) 0 else 1]
			}
			return PatternPortTransform(mapped(0), mapped(1), mapped(2))
		}
	}
}
