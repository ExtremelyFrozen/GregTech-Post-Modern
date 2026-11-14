package com.gregtechceu.gtceu.api.multiblock.pattern.model

import com.gregtechceu.gtceu.api.multiblock.pattern.model.module.PatternPortBoundary

import it.unimi.dsi.fastutil.chars.Char2ObjectMap
import it.unimi.dsi.fastutil.chars.Char2ObjectMaps
import it.unimi.dsi.fastutil.objects.Object2ObjectMap
import org.jspecify.annotations.NullMarked

/** A reusable recursive pattern fragment with an explicit parameter scope. */
@NullMarked
data class PatternFragment @JvmOverloads constructor(
	val parameters: Object2ObjectMap<String, PatternParameter>,
	val anchors: Object2ObjectMap<String, PatternOrigin.Offset>,
	val ports: Object2ObjectMap<String, Port>,
	val body: PatternNode,
	val predicates: Char2ObjectMap<PatternPredicateDefinition> = Char2ObjectMaps.emptyMap(),
) {
	@JvmOverloads
	constructor(
		parameters: Map<String, PatternParameter>,
		anchors: Map<String, PatternOrigin.Offset>,
		ports: Map<String, Port>,
		body: PatternNode,
		predicates: Map<Char, PatternPredicateDefinition> = Char2ObjectMaps.emptyMap(),
	) : this(PatternSourceEntries.objects(parameters), PatternSourceEntries.objects(anchors), PatternSourceEntries.objects(ports), body, PatternSourceEntries.characters(predicates))

	data class Port @JvmOverloads constructor(val offset: PatternOrigin.Offset, val direction: PatternDirection, val up: PatternDirection = if (direction.isHeight()) PatternDirection.FRONT else PatternDirection.UP, val boundary: PatternPortBoundary? = null)
}
