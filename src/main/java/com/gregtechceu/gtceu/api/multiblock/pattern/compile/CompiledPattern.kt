package com.gregtechceu.gtceu.api.multiblock.pattern.compile

import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternDefinition
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternNode

import it.unimi.dsi.fastutil.objects.Object2IntMap
import org.jspecify.annotations.NullMarked

@NullMarked
@JvmRecord
data class CompiledPattern(val definition: PatternDefinition, val cells: List<Cell>, val nodeCounts: Object2IntMap<String>, val maximumRecursionDepth: Int) {
	init {
		require(maximumRecursionDepth >= 0) { "Maximum recursion depth must not be negative" }
	}

	@JvmRecord
	data class Cell(val path: String, val fixed: PatternNode.Fixed, val depthOffset: Int, val heightOffset: Int, val widthOffset: Int)
}
