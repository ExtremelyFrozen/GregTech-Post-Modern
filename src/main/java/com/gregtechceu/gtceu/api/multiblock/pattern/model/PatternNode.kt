package com.gregtechceu.gtceu.api.multiblock.pattern.model

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectList
import it.unimi.dsi.fastutil.objects.ObjectLists
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import org.jspecify.annotations.NullMarked

/** A node in the recursive pattern AST. */
@NullMarked
sealed interface PatternNode {
	/** Explicit translation in definition coordinates, used for placements that cannot be centered. */
	@JvmRecord
	data class Placed(val body: PatternNode, val offset: PatternOrigin.Offset) : PatternNode

	@JvmRecord
	data class Fixed(val layers: ObjectList<ObjectList<String>>) : PatternNode {
		constructor(layers: List<List<String>>) : this(ObjectArrayList<ObjectList<String>>().also { out -> layers.forEach { out.add(ObjectArrayList(it)) } })
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
	data class Sequence(val axis: PatternDirection, val children: ObjectList<PatternNode>) : PatternNode {
		constructor(axis: PatternDirection, children: List<PatternNode>) : this(axis, ObjectArrayList(children))
		init {
			require(children.isNotEmpty()) { "Sequence requires at least one child" }
		}
	}

	@JvmRecord
	data class Repeat @JvmOverloads constructor(val id: String, val axis: PatternDirection, val direction: PatternRepeatDirection, val minimum: Int, val maximum: Int, val body: PatternNode, val minimumParameter: String = "", val maximumParameter: String = "", val axisParameter: String = "") :
		PatternNode {
		init {
			require(id.isNotBlank()) { "Repeat requires an id" }
			require(minimum >= 0 && maximum >= minimum && maximum != Int.MAX_VALUE) {
				"Repeat bounds must be finite and ordered"
			}
		}
	}

	@JvmRecord
	data class Choice @JvmOverloads constructor(val id: String, val alternatives: ObjectList<Alternative>, val priority: ObjectList<String> = ObjectLists.emptyList()) : PatternNode {
		@JvmOverloads
		constructor(id: String, alternatives: List<Alternative>, priority: List<String> = emptyList()) : this(id, ObjectArrayList(alternatives), ObjectArrayList(priority))
		init {
			require(id.isNotBlank() && alternatives.isNotEmpty()) {
				"Choice requires an id and alternatives"
			}
			require(priority.isEmpty() || (priority.size == alternatives.size && ObjectOpenHashSet(priority) == ObjectOpenHashSet<String>().also { out -> alternatives.forEach { out.add(it.id) } })) { "Choice priority must name each branch exactly once" }
		}

		@JvmRecord
		data class Alternative(val id: String, val node: PatternNode) {
			init {
				require(id.isNotBlank()) { "Choice alternatives require an id" }
			}
		}
	}

	@JvmRecord
	data class Fragment(val id: String, val bindings: Object2ObjectMap<String, PatternBinding>) : PatternNode {
		constructor(id: String, bindings: Map<String, PatternBinding>) : this(id, Object2ObjectLinkedOpenHashMap(bindings))
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
