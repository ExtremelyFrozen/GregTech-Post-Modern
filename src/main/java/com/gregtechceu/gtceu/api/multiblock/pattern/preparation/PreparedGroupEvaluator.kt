package com.gregtechceu.gtceu.api.multiblock.pattern.preparation

import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectArrays
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet

/** Only affected independent groups execute rule programs; successful unchanged groups contribute frozen data. */
internal class PreparedGroupEvaluator(private val input: PreparedPatternInput, private val plan: PreparedGroupPlan) {
	private val contributions = ObjectArrayList<PreparedGroupContribution>(plan.groups.size)
	private var nextGroup = 0
	private var stage = Stage.GROUPS
	private var completed: PreparedPatternResult? = null

	fun evaluate(): PreparedPatternResult {
		while (true) advance()?.let { return it }
	}

	/** Consumes at most one dependency group. A null result requests a new executor queue slot. */
	fun advance(): PreparedPatternResult? {
		input.checkpoint()
		when (stage) {
			Stage.FINISHED -> return completed!!
			Stage.FALLBACK -> return finish(PreparedPatternEvaluator(input).evaluate())
			Stage.MERGE -> return merge()?.let(::finish)
			Stage.GROUPS -> Unit
		}
		if (!input.orientationValid) return deferFull()
		if (nextGroup >= plan.groups.size) {
			stage = Stage.MERGE
			return null
		}
		val group = plan.groups[nextGroup++]
		val cached = group.cached
		if (cached != null) {
			contributions.add(cached)
			if (nextGroup == plan.groups.size) stage = Stage.MERGE
			return null
		}
		val result = PreparedPatternEvaluator(input, group.indices, false).evaluate()
		// Full ordered evaluation retains the exact first-failure path and partial state if any group fails.
		if (!result.matched) return deferFull()
		val activeCounters = IntArrayList()
		for (index in result.globalCounts.indices) if (result.globalCounts[index] >= 0 || result.layerCounts[index] >= 0) activeCounters.add(index)
		val counterIds = activeCounters.toIntArray()
		contributions.add(
			PreparedGroupContribution(
				group.indices, result.accepted, result.effects, result.factCounts, result.scopes, result.scopedCounts, result.properties, counterIds,
				IntArray(counterIds.size) { result.globalCounts[counterIds[it]] },
				IntArray(counterIds.size) { result.layerCounts[counterIds[it]] },
			),
		)
		if (nextGroup == plan.groups.size) stage = Stage.MERGE
		return null
	}

	private fun merge(): PreparedPatternResult? {
		val accepted = ObjectArrayList<PreparedAcceptedCell>()
		val effects = ObjectArrayList<PreparedEffect>()
		val counts = Object2IntOpenHashMap<String>()
		val scopes = ObjectLinkedOpenHashSet<String>()
		val scoped = Object2ObjectLinkedOpenHashMap<String, Object2IntOpenHashMap<String>>()
		val properties = Object2ObjectLinkedOpenHashMap<String, PreparedProperty>()
		val global = IntArray(input.counters.size) { -1 }
		val layer = IntArray(input.counters.size) { -1 }
		val finalLayer = if (input.layerRuns.isEmpty()) -1 else input.layerRuns[input.layerRuns.lastIndex]
		for ((indices, accepted1, effects1, factCounts, scopes1, scopedCounts, properties1, counterIds, globalCounts, layerCounts) in contributions) {
			input.checkpoint()
			for (cell in accepted1) accepted.add(cell)
			for (effect in effects1) effects.add(effect)
			for ((fact1, count) in factCounts) counts.addTo(fact1, count)
			for (scope in scopes1) scopes.add(scope)
			for ((scope, fact1, count) in scopedCounts) {
				scoped.computeIfAbsent(scope) { Object2IntOpenHashMap() }.addTo(
					fact1,
					count,
				)
			}
			for (property in properties1) {
				properties[property.key] = property
			}
			for (index in counterIds.indices) {
				if (globalCounts[index] >= 0) {
					val id = counterIds[index]
					global[id] = globalCounts[index]
				}
			}
			if (indices.isNotEmpty() && input.layerRuns[indices[indices.lastIndex]] == finalLayer) {
				for (index in counterIds.indices) {
					if (layerCounts[index] >= 0) {
						val id = counterIds[index]
						layer[id] = layerCounts[index]
					}
				}
			}
		}
		// Rule effects at one cell are already ordered. The stable sort only interleaves distinct groups.
		ObjectArrays.stableSort(accepted.elements(), 0, accepted.size, ACCEPTED_ORDER)
		ObjectArrays.stableSort(effects.elements(), 0, effects.size, EFFECT_ORDER)
		for ((fact, prefix, minimum, maximum) in input.requirements) {
			input.checkpoint()
			if (prefix == "all") {
				if (counts.getInt(fact) !in minimum..maximum) return deferFull()
			} else {
				var found = false
				for (scope in scopes) {
					if (scope.startsWith("$prefix@")) {
						found = true
						if ((scoped[scope]?.getInt(fact) ?: 0) !in minimum..maximum) return deferFull()
					}
				}
				if (!found && 0 !in minimum..maximum) return deferFull()
			}
		}
		return PreparedPatternResult(
			true, emptyArray(), accepted.toTypedArray(), effects.toTypedArray(),
			PreparedContributionArrays.facts(counts), scopes.toTypedArray(),
			PreparedContributionArrays.scoped(scoped),
			properties.values.toTypedArray(), global, layer, if (accepted.isEmpty) -1 else accepted[accepted.size - 1].cell, if (accepted.isEmpty) 0 else accepted[accepted.size - 1].io,
			contributions.toTypedArray(),
		)
	}

	private fun deferFull(): PreparedPatternResult? {
		stage = Stage.FALLBACK
		return null
	}

	private fun finish(result: PreparedPatternResult): PreparedPatternResult {
		completed = result
		stage = Stage.FINISHED
		return result
	}

	private enum class Stage { GROUPS, MERGE, FALLBACK, FINISHED }

	companion object {
		private val ACCEPTED_ORDER = Comparator<PreparedAcceptedCell> { left, right -> left.cell.compareTo(right.cell) }
		private val EFFECT_ORDER = Comparator<PreparedEffect> { left, right -> left.cell.compareTo(right.cell) }
	}
}
