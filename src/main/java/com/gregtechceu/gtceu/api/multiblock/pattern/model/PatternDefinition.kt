package com.gregtechceu.gtceu.api.multiblock.pattern.model

import net.minecraft.resources.ResourceLocation

import it.unimi.dsi.fastutil.chars.Char2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.chars.Char2ObjectMap
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectList
import org.jspecify.annotations.NullMarked

/** Canonical source model shared by Java DSL, JSON and compressed binary definitions. */
@NullMarked
data class PatternDefinition(
	val machine: ResourceLocation,
	val structure: ResourceLocation,
	val axes: PatternAxes,
	val orientation: PatternOrientation,
	val origin: PatternOrigin,
	val parameters: Object2ObjectMap<String, PatternParameter>,
	val fragments: Object2ObjectMap<String, PatternFragment>,
	val predicates: Char2ObjectMap<PatternPredicateDefinition>,
	val body: PatternNode,
	val constraints: ObjectList<PatternConstraint>,
) {
	/** Collection conversion is confined to the source/codec interoperability boundary. */
	constructor(
		machine: ResourceLocation,
		structure: ResourceLocation,
		axes: PatternAxes,
		orientation: PatternOrientation,
		origin: PatternOrigin,
		parameters: Map<String, PatternParameter>,
		fragments: Map<String, PatternFragment>,
		predicates: Map<Char, PatternPredicateDefinition>,
		body: PatternNode,
		constraints: List<PatternConstraint>,
	) : this(machine, structure, axes, orientation, origin, Object2ObjectLinkedOpenHashMap(parameters), Object2ObjectLinkedOpenHashMap(fragments), Char2ObjectLinkedOpenHashMap(predicates), body, ObjectArrayList(constraints))
}
