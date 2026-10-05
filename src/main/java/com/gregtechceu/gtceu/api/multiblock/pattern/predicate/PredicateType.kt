package com.gregtechceu.gtceu.api.multiblock.pattern.predicate

import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternPredicateDefinition

import java.util.function.Function

/** Registry entry that turns a stable predicate definition into the runtime predicate. */
@JvmRecord
data class PredicateType(val compiler: Function<PatternPredicateDefinition, PatternPredicate>) {
	fun compile(definition: PatternPredicateDefinition): PatternPredicate = compiler.apply(definition)
}
