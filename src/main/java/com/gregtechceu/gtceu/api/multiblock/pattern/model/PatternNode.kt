package com.gregtechceu.gtceu.api.multiblock.pattern.model

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import org.jspecify.annotations.NullMarked

/** A node in the recursive pattern AST. */
@NullMarked
sealed interface PatternNode {
	@JvmRecord
	data class Fixed(val layers: List<List<String>>) : PatternNode {
		init {
			require(layers.isNotEmpty()) { "Fixed pattern must contain at least one layer" }
			var height = -1
			var width = -1
			layers.forEach { layer ->
				require(layer.isNotEmpty()) { "Fixed pattern layers must not be empty" }
				if (height < 0) height = layer.size
				require(height == layer.size) { "Fixed pattern layers must have equal heights" }
				layer.forEach { row ->
					require(row.isNotEmpty()) { "Fixed pattern rows must not be empty" }
					if (width < 0) width = row.length
					require(width == row.length) { "Fixed pattern rows must have equal widths" }
				}
			}
		}

		fun depth(): Int = layers.size

		fun height(): Int = layers.first().size

		fun width(): Int = layers.first().first().length

		companion object {
			@JvmStatic
			fun fromSlices(vararg slices: Array<String>): Fixed {
				val layers = ObjectArrayList<List<String>>(slices.size)
				slices.forEach { slice ->
					val rows = ObjectArrayList<String>(slice.size)
					slice.forEach(rows::add)
					layers.add(rows)
				}
				return Fixed(layers)
			}
		}
	}

	@JvmRecord
	data class Sequence(val axis: PatternDirection, val children: List<PatternNode>) : PatternNode {
		init {
			require(children.isNotEmpty()) { "Sequence requires at least one child" }
		}
	}

	@JvmRecord
	data class Repeat(val id: String, val axis: PatternDirection, val direction: PatternRepeatDirection, val minimum: Int, val maximum: Int, val body: PatternNode) : PatternNode {
		init {
			require(id.isNotBlank()) { "Repeat requires an id" }
			require(minimum >= 0 && maximum >= minimum && maximum != Int.MAX_VALUE) {
				"Repeat bounds must be finite and ordered"
			}
		}
	}

	@JvmRecord
	data class Choice(val id: String, val alternatives: List<Alternative>) : PatternNode {
		init {
			require(id.isNotBlank() && alternatives.isNotEmpty()) {
				"Choice requires an id and alternatives"
			}
		}

		@JvmRecord
		data class Alternative(val id: String, val node: PatternNode) {
			init {
				require(id.isNotBlank()) { "Choice alternatives require an id" }
			}
		}
	}

	@JvmRecord
	data class Fragment(val id: String, val bindings: Map<String, PatternBinding>) : PatternNode {
		init {
			require(id.isNotBlank()) { "Fragment reference requires an id" }
			bindings.forEach { (name, value) ->
				require(name.isNotBlank()) {
					"Fragment bindings must have non-empty names and values"
				}
			}
		}
	}
}
