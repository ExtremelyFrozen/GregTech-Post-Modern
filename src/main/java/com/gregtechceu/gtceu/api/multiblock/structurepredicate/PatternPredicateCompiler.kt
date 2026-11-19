package com.gregtechceu.gtceu.api.multiblock.structurepredicate

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternPredicateDefinition

import net.minecraft.resources.ResourceLocation

/** Every serialized or Java definition is dispatched by its registered codec/compiler contract. */
object PatternPredicateCompiler {
	@JvmStatic
	fun controller(machine: ResourceLocation): PatternPredicate = compile(
		PatternPredicateDefinition("controller", "", mapOf("machine" to machine.toString()), emptyList()),
	)

	@JvmStatic
	fun compile(definition: PatternPredicateDefinition): PatternPredicate {
		val id = if (definition.type.contains(':')) ResourceLocation.parse(definition.type) else GTCEu.id(definition.type)
		return PredicateRegistry.require(id).compile(definition)
	}

	fun observe(predicate: PatternPredicate, state: MultiblockState): PatternObservation = predicate.observePosition(state)
}
