package com.gregtechceu.gtceu.api.multiblock.pattern.model

import it.unimi.dsi.fastutil.objects.Object2ObjectMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectList
import org.jspecify.annotations.NullMarked

/** A serialized predicate reference and its stable fact aliases. */
@NullMarked
data class PatternPredicateDefinition(val type: String, val name: String, val properties: Object2ObjectMap<String, Any>, val facts: ObjectList<String>) {
	constructor(type: String, name: String, properties: Map<String, Any>, facts: List<String>) : this(type, name, PatternSourceEntries.objects(properties), ObjectArrayList(facts))
}
