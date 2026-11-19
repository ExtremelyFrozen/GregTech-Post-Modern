package com.gregtechceu.gtceu.api.multiblock.pattern.compile

import com.gregtechceu.gtceu.api.multiblock.pattern.layout.PatternLayoutResolver
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternDefinition

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap

/** Java and resource sources use the same compiled recursive plan. */
class PatternCompiler {
	fun compile(definition: PatternDefinition): CompiledPattern {
		val layout = PatternLayoutResolver(definition)
		val counts = Object2IntOpenHashMap<String>()
		layout.repeatDescriptors().forEach { counts[it.path] = it.maximum }
		val initial = layout.expand(IntArray(layout.repeatDescriptors().size) { layout.repeatDescriptors()[it].minimum }, emptyMap())
		return CompiledPattern(definition, initial.cells, counts, layout.maximumDepth, layout)
	}
}
