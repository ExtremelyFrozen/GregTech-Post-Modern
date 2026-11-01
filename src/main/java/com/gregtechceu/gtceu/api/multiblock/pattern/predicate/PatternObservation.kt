package com.gregtechceu.gtceu.api.multiblock.pattern.predicate

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectMap
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import it.unimi.dsi.fastutil.objects.ObjectSet
import it.unimi.dsi.fastutil.objects.ObjectSets

/**
 * A position observation owns its facts and properties; failed branches never publish either.
 * Properties are immutable scalar structure invariants: every successful position that supplies the same key
 * must supply an equal value. Position-dependent information belongs in facts or explicit aggregation state.
 * Extensions should use qualified keys; all property keys share this consistency contract.
 */
@JvmRecord
data class PatternObservation(val matched: Boolean, val facts: ObjectSet<String>, val properties: Object2ObjectMap<String, Any>) {
	fun withAliases(aliases: Collection<String>): PatternObservation = if (!matched || aliases.isEmpty()) this else of(true, ObjectOpenHashSet(facts).also { it.addAll(aliases) }, properties)

	companion object {
		@JvmStatic
		@JvmOverloads
		fun of(matched: Boolean, facts: Collection<String> = ObjectSets.emptySet(), properties: Map<String, Any> = Object2ObjectMaps.emptyMap()): PatternObservation =
			PatternObservation(matched, if (matched) ObjectSets.unmodifiable(ObjectOpenHashSet(facts)) else ObjectSets.emptySet(), if (matched) Object2ObjectMaps.unmodifiable(Object2ObjectLinkedOpenHashMap(properties)) else Object2ObjectMaps.emptyMap())
	}
}
