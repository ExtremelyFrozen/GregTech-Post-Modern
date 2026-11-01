package com.gregtechceu.gtceu.api.multiblock.pattern.model

import it.unimi.dsi.fastutil.chars.Char2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.chars.Char2ObjectMap
import it.unimi.dsi.fastutil.chars.Char2ObjectMaps
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectMap
import org.jspecify.annotations.NullMarked

/** A reusable recursive pattern fragment with an explicit parameter scope. */
@NullMarked
@JvmRecord
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
		predicates: Map<Char, PatternPredicateDefinition> = emptyMap(),
	) : this(Object2ObjectLinkedOpenHashMap(parameters), Object2ObjectLinkedOpenHashMap(anchors), Object2ObjectLinkedOpenHashMap(ports), body, Char2ObjectLinkedOpenHashMap(predicates))

	@JvmRecord
	data class Port(val offset: PatternOrigin.Offset, val direction: PatternDirection)
}
