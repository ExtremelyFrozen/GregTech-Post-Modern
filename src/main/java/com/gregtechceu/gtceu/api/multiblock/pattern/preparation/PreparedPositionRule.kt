package com.gregtechceu.gtceu.api.multiblock.pattern.preparation

import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.PatternObservation

import it.unimi.dsi.fastutil.objects.ObjectSet
import it.unimi.dsi.fastutil.objects.ObjectSets

/**
 * Optional preparation capability for a custom structure predicate. The server thread calls every member;
 * only detached booleans and interned value IDs enter the worker program. An observation must not depend on
 * current counters, mutable match facts or another candidate. Declare every world read through the predicate's
 * position dependency contract. Its acceptPosition must apply this captured observation without rejecting it or
 * reading the world again. Rules that cannot meet these constraints retain synchronous full matching.
 */
interface PreparedPositionRule {
	/** Captures one loaded position without modifying world, counters, parts or published match facts. */
	fun preparePosition(state: MultiblockState): PatternObservation

	/** An observation property that must have the same value throughout the interpretation; empty means none. */
	val consistencyProperty: String get() = ""

	/** Stable names for shared aggregate contributions; matching names merge their incremental dependency groups. */
	val aggregationKeys: ObjectSet<String> get() = ObjectSets.emptySet()
}
