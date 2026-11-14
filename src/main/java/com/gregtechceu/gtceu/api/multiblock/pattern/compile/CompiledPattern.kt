package com.gregtechceu.gtceu.api.multiblock.pattern.compile

import com.gregtechceu.gtceu.api.multiblock.pattern.layout.PatternLayout
import com.gregtechceu.gtceu.api.multiblock.pattern.layout.PatternLayoutCell
import com.gregtechceu.gtceu.api.multiblock.pattern.layout.PatternLayoutResolver
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternDefinition

import it.unimi.dsi.fastutil.objects.Object2IntMap
import it.unimi.dsi.fastutil.objects.Object2IntMaps
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectList

data class CompiledPattern(val definition: PatternDefinition, val cells: ObjectList<PatternLayoutCell>, val nodeCounts: Object2IntMap<String>, val maximumRecursionDepth: Int, val layout: PatternLayoutResolver) {
	fun expand(repetitions: IntArray, choices: Map<String, String>): PatternLayout {
		val values = Object2IntOpenHashMap<String>()
		for (measurement in definition.measurements) {
			val key = "@measure:${measurement.parameter}"
			val encoded = choices[key]
			val value = encoded?.toInt() ?: measurement.minimum
			values.put(measurement.parameter, value)
		}
		return layout.expand(repetitions, choices, Object2IntMaps.emptyMap(), values)
	}
}
