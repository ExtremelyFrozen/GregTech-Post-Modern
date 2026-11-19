package com.gregtechceu.gtceu.api.multiblock.pattern.constraint

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectMap
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap

/** Reversible property bindings for a single candidate search; values use structural equality. */
class PropertyBindings {
	private val counts = Object2ObjectOpenHashMap<String, Object2IntOpenHashMap<Any>>()

	fun accepts(properties: Object2ObjectMap<String, Any>): Boolean = properties.all { (key, value) ->
		val active = counts[key]
		active == null || active.containsKey(value)
	}

	fun add(properties: Object2ObjectMap<String, Any>, amount: Int) {
		for ((key, value) in properties) {
			val values = counts.computeIfAbsent(key) { Object2IntOpenHashMap() }
			val remaining = values.addTo(value, amount) + amount
			if (remaining == 0) values.removeInt(value)
			if (values.isEmpty()) counts.remove(key)
		}
	}

	fun values(): Object2ObjectMap<String, Any> = Object2ObjectOpenHashMap<String, Any>().also { result ->
		counts.forEach { (key, values) -> result[key] = values.keys.first() }
	}
}
