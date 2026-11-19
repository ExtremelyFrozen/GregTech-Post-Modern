package com.gregtechceu.gtceu.api.multiblock.pattern.model

import com.gregtechceu.gtceu.api.multiblock.pattern.model.expression.PatternSizeConstraint
import com.gregtechceu.gtceu.api.multiblock.pattern.model.measurement.PatternMeasurement

import net.minecraft.resources.ResourceLocation

import it.unimi.dsi.fastutil.chars.Char2ObjectMap
import it.unimi.dsi.fastutil.objects.Object2ObjectMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectList
import it.unimi.dsi.fastutil.objects.ObjectLists
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
	val measurements: ObjectList<PatternMeasurement> = ObjectLists.emptyList(),
	val sizeConstraints: ObjectList<PatternSizeConstraint> = ObjectLists.emptyList(),
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
		measurements: List<PatternMeasurement> = ObjectLists.emptyList(),
		sizeConstraints: List<PatternSizeConstraint> = ObjectLists.emptyList(),
	) : this(machine, structure, axes, orientation, origin, PatternSourceEntries.objects(parameters), PatternSourceEntries.objects(fragments), PatternSourceEntries.characters(predicates), body, ObjectArrayList(constraints), ObjectArrayList(measurements), ObjectArrayList(sizeConstraints))
}
