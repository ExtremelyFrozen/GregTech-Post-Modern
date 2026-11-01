package com.gregtechceu.gtceu.data.pattern

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition
import com.gregtechceu.gtceu.api.multiblock.pattern.dsl.PatternBuilder
import com.gregtechceu.gtceu.api.multiblock.pattern.match.MultiBlockPattern
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternDefinition
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternDirection
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternNode
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternPredicateDefinition
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.PatternPredicate
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.PatternPredicateCompiler

import it.unimi.dsi.fastutil.chars.Char2ObjectLinkedOpenHashMap

/** Source adaptation never flattens recursive geometry or changes its matching semantics. */
object PatternDefinitionAdapter {
	@JvmStatic
	fun appendToBuilder(builder: PatternBuilder, definition: PatternDefinition) {
		builder.node(definition.body)
		definition.fragments.forEach { (id, fragment) -> builder.fragment(id, fragment) }
		definition.predicates.forEach { (symbol, predicate) -> builder.where(symbol, PatternPredicateCompiler.compile(predicate)) }
		definition.constraints.forEach(builder::constraint)
	}

	@JvmStatic
	fun withBody(source: PatternDefinition, nodes: List<PatternNode>): PatternDefinition = source.copy(body = if (nodes.size == 1) nodes.first() else PatternNode.Sequence(PatternDirection.FRONT, nodes))

	@JvmStatic
	fun compile(owner: MultiblockMachineDefinition, key: StructurePatternKey, baseline: MultiBlockPattern, definition: PatternDefinition): MultiBlockPattern {
		require(definition.machine == owner.id || definition.machine == definition.structure) { "Pattern machine does not match $key" }
		val predicates = Char2ObjectLinkedOpenHashMap(definition.predicates)
		predicates['~'] = PatternPredicateDefinition("java", "", mapOf("runtime" to PatternPredicate.controller(owner)), emptyList())
		predicates['@'] = predicates['~']!!
		return MultiBlockPattern.fromDefinition(definition.copy(machine = owner.id, predicates = predicates)).also { it.condition = baseline.condition }
	}
}
