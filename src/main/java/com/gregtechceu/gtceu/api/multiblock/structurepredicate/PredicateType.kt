package com.gregtechceu.gtceu.api.multiblock.structurepredicate

import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternPredicateDefinition

import com.mojang.serialization.JavaOps
import com.mojang.serialization.MapCodec

import java.util.function.Function

/**
 * Decodes one registered predicate and compiles it for both matching and planning.
 * The compiler supplies pure position/candidate observations, standard facts and consistent properties through
 * the returned predicate. Its candidates and diagnose method are also used by previews and match diagnostics.
 * Registration is process-wide; compiled predicates must support concurrent read-only observation.
 */
class PredicateType<T>(val codec: MapCodec<T>, val compiler: Function<T, PatternPredicate>) {
	fun compile(definition: PatternPredicateDefinition): PatternPredicate = compiler.apply(
		codec.codec().parse(JavaOps.INSTANCE, definition.properties).getOrThrow { IllegalArgumentException(it) },
	).withFacts(definition.facts)
}
