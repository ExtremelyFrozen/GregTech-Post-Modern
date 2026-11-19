package com.gregtechceu.gtceu.api.multiblock.pattern.model

import com.gregtechceu.gtceu.api.multiblock.pattern.model.expression.PatternIntegerExpression
import com.gregtechceu.gtceu.api.multiblock.pattern.model.module.PatternModuleSlot

import it.unimi.dsi.fastutil.objects.Object2ObjectMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectList
import it.unimi.dsi.fastutil.objects.ObjectLists
import org.jspecify.annotations.NullMarked

/** A node in the recursive pattern AST. */
@NullMarked
sealed interface PatternNode {
	data class Assembly(val root: Fragment, val slots: ObjectList<PatternModuleSlot>) : PatternNode {
		constructor(root: Fragment, slots: List<PatternModuleSlot>) : this(root, ObjectArrayList(slots))
	}

	/** Explicit translation in definition coordinates, used for placements that cannot be centered. */
	data class Placed(val body: PatternNode, val offset: PatternOrigin.Offset) : PatternNode

	data class Fixed(val layers: ObjectList<ObjectList<String>>) : PatternNode {
		constructor(layers: List<List<String>>) : this(ObjectArrayList<ObjectList<String>>().also { out -> layers.forEach { out.add(ObjectArrayList(it)) } })

		fun depth(): Int = layers.size

		fun height(): Int = layers.first().size

		fun width(): Int = layers.first().first().length

		companion object {
			@JvmStatic
			fun fromSlices(vararg slices: Array<String>): Fixed {
				val layers = ObjectArrayList<ObjectList<String>>(slices.size)
				slices.forEach { slice ->
					val rows = ObjectArrayList<String>(slice.size)
					slice.forEach(rows::add)
					layers.add(rows)
				}
				return Fixed(layers)
			}
		}
	}

	data class Sequence(val axis: PatternDirection, val children: ObjectList<PatternNode>) : PatternNode {
		constructor(axis: PatternDirection, children: List<PatternNode>) : this(axis, ObjectArrayList(children))
	}

	data class Repeat @JvmOverloads constructor(
		val id: String,
		val axis: PatternDirection,
		val direction: PatternRepeatDirection,
		val minimum: Int,
		val maximum: Int,
		val body: PatternNode,
		val minimumParameter: String = "",
		val maximumParameter: String = "",
		val axisParameter: String = "",
		val count: Count? = null,
		val countExpression: PatternIntegerExpression? = null,
	) : PatternNode {
		/** Reuses a root dimension; offsets and integer division place both odd and even sized centers. */
		data class Count(val parameter: String, val offset: Int = 0, val divisor: Int = 1) {
			fun resolve(value: Int): Int = Math.floorDiv(Math.addExact(value, offset), divisor)
		}
	}

	data class Choice @JvmOverloads constructor(val id: String, val alternatives: ObjectList<Alternative>, val priority: ObjectList<String> = ObjectLists.emptyList()) : PatternNode {
		@JvmOverloads
		constructor(id: String, alternatives: List<Alternative>, priority: List<String> = ObjectLists.emptyList()) : this(id, ObjectArrayList(alternatives), ObjectArrayList(priority))

		data class Alternative(val id: String, val node: PatternNode)
	}

	data class Fragment(val id: String, val bindings: Object2ObjectMap<String, PatternBinding>) : PatternNode {
		constructor(id: String, bindings: Map<String, PatternBinding>) : this(id, PatternSourceEntries.objects(bindings))
	}
}
