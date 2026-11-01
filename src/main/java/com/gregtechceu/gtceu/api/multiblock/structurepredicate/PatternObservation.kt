package com.gregtechceu.gtceu.api.multiblock.structurepredicate

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
data class PatternObservation(val matched: Boolean, val facts: ObjectSet<String>, val properties: Object2ObjectMap<String, Any>, val positionMatch: PositionMatch?) {
	constructor(matched: Boolean, facts: ObjectSet<String>, properties: Object2ObjectMap<String, Any>) : this(matched, facts, properties, null)

	/** A rule-owned snapshot for this position only; it contains data, never a shared cache or an effects runner. */
	sealed interface PositionMatch

	internal fun withPositionMatch(match: PositionMatch): PatternObservation = copy(positionMatch = match)

	fun withAliases(aliases: Collection<String>): PatternObservation = if (!matched || aliases.isEmpty()) this else copy(facts = ObjectOpenHashSet(facts).also { it.addAll(aliases) })

	companion object {
		@JvmStatic
		@JvmOverloads
		fun of(matched: Boolean, facts: Collection<String> = ObjectSets.emptySet(), properties: Map<String, Any> = Object2ObjectMaps.emptyMap()): PatternObservation =
			PatternObservation(matched, if (matched) (facts as? ObjectSet<String> ?: ObjectOpenHashSet(facts)) else ObjectSets.emptySet(), if (matched) (properties as? Object2ObjectMap<String, Any> ?: Object2ObjectLinkedOpenHashMap(properties)) else Object2ObjectMaps.emptyMap())
	}
}
