package com.gregtechceu.gtceu.api.multiblock.pattern.preparation

import it.unimi.dsi.fastutil.objects.Object2IntMap
import it.unimi.dsi.fastutil.objects.Object2IntMaps
import it.unimi.dsi.fastutil.objects.Object2ObjectMap
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps
import it.unimi.dsi.fastutil.objects.ObjectArrayList

/** Copies primitive counter entries directly into the detached result arrays without boxed intermediate lists. */
internal object PreparedContributionArrays {
	fun facts(counts: Object2IntMap<String>): Array<PreparedFactCount> {
		val entries = Object2IntMaps.fastIterator(counts)
		return Array(counts.size) {
			val entry = entries.next()
			PreparedFactCount(entry.key, entry.intValue)
		}
	}

	fun scoped(scopes: Object2ObjectMap<String, out Object2IntMap<String>>): Array<PreparedScopedCount> {
		val output = ObjectArrayList<PreparedScopedCount>()
		val scopeEntries = Object2ObjectMaps.fastIterator(scopes)
		while (scopeEntries.hasNext()) {
			val scope = scopeEntries.next()
			val counts = Object2IntMaps.fastIterator(scope.value)
			while (counts.hasNext()) {
				val entry = counts.next()
				output.add(PreparedScopedCount(scope.key, entry.key, entry.intValue))
			}
		}
		return output.toTypedArray()
	}
}
