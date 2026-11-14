package com.gregtechceu.gtceu.data.pattern

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition
import com.gregtechceu.gtceu.api.multiblock.pattern.dsl.PatternBuilder
import com.gregtechceu.gtceu.api.multiblock.pattern.match.MultiBlockPattern
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternDefinition
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternNode

/** Resolves canonical definitions into the single runtime matcher used by all consumers. */
object StructurePatternResolver {
	@JvmStatic
	fun appendDefinition(builder: PatternBuilder, key: StructurePatternKey): PatternBuilder {
		PatternDefinitionAdapter.appendToBuilder(builder, loadDefinition(key))
		return builder
	}

	@JvmStatic
	fun loadDefinition(key: StructurePatternKey): PatternDefinition = StructureCache.getPatternDefinition(key)
		?: error("Pattern definition for $key was not found")

	@JvmStatic
	fun rebuildPattern(owner: MultiblockMachineDefinition, key: StructurePatternKey, baseline: MultiBlockPattern, definition: PatternDefinition): MultiBlockPattern = PatternDefinitionAdapter.compile(owner, key, baseline, definition)

	@JvmStatic
	fun rebuildRuntimePattern(owner: MultiblockMachineDefinition, key: StructurePatternKey, baseline: MultiBlockPattern, nodes: List<PatternNode>): MultiBlockPattern {
		val source = loadDefinition(key)
		return PatternDefinitionAdapter.compile(owner, key, baseline, PatternDefinitionAdapter.withBody(source, nodes))
	}
}
