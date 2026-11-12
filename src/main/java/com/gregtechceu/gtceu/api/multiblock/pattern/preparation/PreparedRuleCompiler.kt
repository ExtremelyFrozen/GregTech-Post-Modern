package com.gregtechceu.gtceu.api.multiblock.pattern.preparation

import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.*
import com.gregtechceu.gtceu.common.machine.multiblock.part.RotorHolderPartMachine

import it.unimi.dsi.fastutil.longs.LongSet
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectList
import it.unimi.dsi.fastutil.objects.ObjectLists
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap

/** Main-thread adapter for the closed set of built-in rules. Native objects never enter its emitted tree. */
internal class PreparedRuleCompiler(private val capturedBlocks: CapturedPatternBlocks, private val confirmed: PreparedRuleCompiler? = null) {
	private val counterOffset = confirmed?.counterCount ?: 0
	private val valueOffset = confirmed?.valueCount ?: 0
	private val nodeOffset = confirmed?.nodeCount ?: 0
	private val counters = ObjectArrayList<PreparedCounter>()
	private val counterKeys = ObjectArrayList<Any>()
	private val ruleCounters = Reference2IntOpenHashMap<PredicateRule>().also { it.defaultReturnValue(-1) }
	private val structureCounters = Object2IntOpenHashMap<RestrictedPredicate>().also { it.defaultReturnValue(-1) }
	private val values = ObjectArrayList<Any>()
	private val valueIds = Object2IntOpenHashMap<Any>().also { it.defaultReturnValue(-1) }
	private val references = Reference2IntOpenHashMap<Any>().also { it.defaultReturnValue(-1) }
	private val nativeNodes = ObjectArrayList<NativePreparedNode>()
	private val counterCount: Int get() = counterOffset + counters.size
	private val valueCount: Int get() = valueOffset + values.size
	val nodeCount: Int get() = nodeOffset + nativeNodes.size

	fun extend(blocks: CapturedPatternBlocks): PreparedRuleCompiler = PreparedRuleCompiler(blocks, this)

	fun counterDefinitions(): Array<PreparedCounter> {
		val definitions = arrayOfNulls<PreparedCounter>(counterCount)
		var layer: PreparedRuleCompiler? = this
		while (layer != null) {
			for ((index, element) in layer.counters.withIndex()) definitions[layer.counterOffset + index] = element
			layer = layer.confirmed
		}
		return Array(definitions.size) { definitions[it]!! }
	}

	fun value(id: Int): Any {
		var layer = this
		while (id < layer.valueOffset) layer = layer.confirmed!!
		return layer.values[id - layer.valueOffset]
	}

	private fun native(id: Int): NativePreparedNode {
		var layer = this
		while (id < layer.nodeOffset) layer = layer.confirmed!!
		return layer.nativeNodes[id - layer.nodeOffset]
	}

	private fun valueId(value: Any): Int {
		var layer: PreparedRuleCompiler? = this
		while (layer != null) {
			val existing = layer.valueIds.getInt(value)
			if (existing >= 0) return existing
			layer = layer.confirmed
		}
		val id = referenceId(value)
		valueIds[value] = id
		return id
	}

	private fun referenceId(value: Any): Int {
		var layer: PreparedRuleCompiler? = this
		while (layer != null) {
			val existing = layer.references.getInt(value)
			if (existing >= 0) return existing
			layer = layer.confirmed
		}
		return valueCount.also { id ->
			values.add(value)
			references[value] = id
		}
	}

	fun compile(predicate: PatternPredicate, state: MultiblockState, loaded: Boolean): PreparedNode {
		val id = reserve(NativePreparedNode(pattern = predicate))
		if (predicate is CombinedPredicate) {
			val expression = combined(predicate, state, loaded, false)
			val rules = Array(predicate.limited.size + predicate.common.size) { index -> rule(ruleAt(predicate, index), index < predicate.limited.size, expression) }
			return PreparedNode.Observed(id, expression, rules, predicate.facts.toTypedArray()).also { native(id).node = it }
		}
		val rules = Array(predicate.limited.size + predicate.common.size) { index ->
			val rule = ruleAt(predicate, index)
			val source = rule.source
			val child = if (source != null) compile(source, state, loaded) else combined(rule.observedPredicate as CombinedPredicate, state, loaded, true)
			rule(rule, index < predicate.limited.size, child)
		}
		return PreparedNode.Pattern(id, rules, predicate.facts.toTypedArray()).also { native(id).node = it }
	}

	private fun combined(predicate: CombinedPredicate, state: MultiblockState, loaded: Boolean, aliases: Boolean): PreparedNode {
		val id = reserve(NativePreparedNode(pattern = predicate))
		val branches = predicate.preparedBranches
		return PreparedNode.Combined(id, predicate.preparedSelection, predicate.preparedPriorities, Array(branches.size) { compile(branches[it], state, loaded) }, if (aliases) predicate.facts.toTypedArray() else emptyArray()).also { native(id).node = it }
	}

	private fun compile(source: StructurePredicate, state: MultiblockState, loaded: Boolean): PreparedNode {
		val id = reserve(NativePreparedNode(source = source))
		val node: PreparedNode = when (source) {
			is RestrictedPredicate -> PreparedNode.Restricted(id, counter(source), compile(source.predicate, state, loaded))

			is DataHatchOrPredicate -> {
				val alternatives = source.alternatives(state)
				val data = if (alternatives.size > 1) alternatives[0] else null
				PreparedNode.DataHatch(id, data?.let { compile(it, state, loaded) }, compile(source.alternative, state, loaded))
			}

			is AlternativePredicate -> {
				val alternatives = source.alternatives(state)
				native(id).alternatives = alternatives
				PreparedNode.Alternative(id, Array(alternatives.size) { compile(alternatives[it], state, loaded) })
			}

			else -> {
				// Aggregate dependencies are interpreted by the worker; capture their independent leaf observation now.
				val observation = if (!loaded) {
					PatternObservation.of(false)
				} else if (source is RotorHolderPredicate) {
					val rotor = state.blockEntity as? RotorHolderPartMachine
					val front = rotor?.let { state.pos.relative(it.self().frontFacing) }
					PatternObservation.of(rotor != null && rotor.tier == source.tier && capturedBlocks[front!!.asLong()]?.block?.isAir == true)
				} else if (source is PreparedPositionRule) {
					source.preparePosition(state)
				} else {
					source.observePosition(state)
				}
				native(id).observation = observation
				val consistency = when (source) {
					is PreparedPositionRule -> source.consistencyProperty
					is HeatingCoilPredicate -> "CoilType"
					is CleanroomFilterPredicate -> "FilterType"
					else -> ""
				}
				val aggregation = when (source) {
					is PreparedPositionRule -> source.aggregationKeys.toTypedArray()
					is PowerSubstationBatteryPredicate -> arrayOf("battery")
					is CharcoalLogOrAirPredicate -> arrayOf("logPos")
					is CleanroomInnerPredicate -> arrayOf("cleanroomReceiver")
					else -> emptyArray()
				}
				val properties = Object2ObjectMaps.fastIterator(observation.properties)
				val detached = Array(observation.properties.size) {
					val property = properties.next()
					PreparedProperty(property.key, valueId(property.value), referenceId(property.value))
				}
				PreparedNode.Leaf(id, observation.matched, observation.facts.toTypedArray(), detached, consistency, id, aggregation)
			}
		}
		native(id).node = node
		return node
	}

	private fun rule(rule: PredicateRule, limited: Boolean, source: PreparedNode): PreparedRule = PreparedRule(source, counter(rule), limited, PatternPreparation.encodeIO(rule.io), rule.disableRenderFormed, rule.slotName)

	private fun counter(rule: PredicateRule): Int {
		if (rule.minCount < 0 && rule.maxCount < 0 && rule.minLayerCount < 0 && rule.maxLayerCount < 0) return -1
		var layer: PreparedRuleCompiler? = this
		while (layer != null) {
			val existing = layer.ruleCounters.getInt(rule)
			if (existing >= 0) return existing
			layer = layer.confirmed
		}
		val id = counterCount
		counters.add(PreparedCounter(rule.minCount, rule.maxCount, rule.minLayerCount, rule.maxLayerCount))
		counterKeys.add(rule)
		ruleCounters[rule] = id
		return id
	}

	private fun counter(source: RestrictedPredicate): Int {
		if (source.minCount.isEmpty && source.maxCount.isEmpty && source.minCountByLayer.isEmpty && source.maxCountByLayer.isEmpty) return -1
		var layer: PreparedRuleCompiler? = this
		while (layer != null) {
			val existing = layer.structureCounters.getInt(source)
			if (existing >= 0) return existing
			layer = layer.confirmed
		}
		val id = counterCount
		counters.add(PreparedCounter(source.minCount.orElse(-1), source.maxCount.orElse(-1), source.minCountByLayer.orElse(-1), source.maxCountByLayer.orElse(-1)))
		counterKeys.add(source)
		structureCounters[source] = id
		return id
	}

	private fun reserve(node: NativePreparedNode): Int = nodeCount.also { nativeNodes.add(node) }

	fun applyLeaf(id: Int, state: MultiblockState) {
		val native = native(id)
		native.source!!.acceptPosition(state, native.observation!!)
	}

	fun restoreCounts(result: PreparedPatternResult, state: MultiblockState) {
		var layer: PreparedRuleCompiler? = this
		while (layer != null) {
			for ((localIndex, element) in layer.counterKeys.withIndex()) {
				val index = layer.counterOffset + localIndex
				when (val key = element) {
					is PredicateRule -> {
						if (result.globalCounts[index] >= 0) state.globalCount[key] = result.globalCounts[index]
						if (result.layerCounts[index] >= 0) state.layerCount[key] = result.layerCounts[index]
					}

					is StructurePredicate -> {
						if (result.globalCounts[index] >= 0) state.structureGlobalCount[key] = result.globalCounts[index]
						if (result.layerCounts[index] >= 0) state.structureLayerCount[key] = result.layerCounts[index]
					}
				}
			}
			layer = layer.confirmed
		}
	}

	fun restore(observation: PreparedObservation): PatternObservation {
		val native = native(observation.node)
		val properties = Object2ObjectLinkedOpenHashMap<String, Any>()
		for ((key, _, reference) in observation.properties) properties[key] = value(reference)
		val restored = PatternObservation.of(observation.matched, ObjectArrayList.wrap(observation.facts), properties)
		val children = ObjectArrayList<PatternObservation>(observation.children.size)
		for (child in observation.children) children.add(restore(child))
		return when (val node = native.node!!) {
			is PreparedNode.Leaf -> if (observation.matched) native.observation!!.copy(facts = restored.facts, properties = restored.properties) else restored

			is PreparedNode.Pattern -> native.pattern!!.restorePreparedPosition(restored, children, observation.available)

			is PreparedNode.Observed -> children.firstOrNull()?.copy(facts = restored.facts, properties = restored.properties)
				?: restored

			is PreparedNode.Restricted -> if (children.isEmpty) restored else (native.source as RestrictedPredicate).restorePreparedPosition(restored, children[0])

			is PreparedNode.Alternative -> AlternativePredicate.restorePreparedPosition(
				restored,
				ObjectArrayList<Pair<StructurePredicate, PatternObservation>>().also { out ->
					for ((index, element) in children.withIndex()) out.add(native.alternatives[index] to element)
				},
			)

			is PreparedNode.Combined -> if (!observation.matched) restored else (native.pattern as CombinedPredicate).restorePreparedExpression(restored, observation.selected, children)

			is PreparedNode.DataHatch -> {
				if (children.isEmpty) {
					restored
				} else {
					val source = native.source as DataHatchOrPredicate
					val selected = if (observation.selected[0] == 0) node.data!! else node.alternative
					val selectedSource = native(selected.id).source!!
					val required = if (observation.selected[0] == 1) node.data?.let { native(it.id).source as RestrictedPredicate } else null
					source.restorePreparedPosition(restored, selectedSource, children[0], required)
				}
			}
		}
	}

	companion object {
		private fun ruleAt(predicate: PatternPredicate, index: Int): PredicateRule = if (index < predicate.limited.size) predicate.limited[index] else predicate.common[index - predicate.limited.size]

		fun trusted(predicate: PatternPredicate, diagnostic: Boolean = false): Boolean {
			if (predicate.javaClass != PatternPredicate::class.java && predicate.javaClass != PredicateController::class.java && predicate.javaClass != CombinedPredicate::class.java) return false
			if (!predicate.supportsPreparedDirection()) return false
			if (predicate is CombinedPredicate) {
				for (branch in predicate.preparedBranches) if (!trusted(branch, diagnostic)) return false
				for (index in 0 until predicate.limited.size + predicate.common.size) {
					val rule = ruleAt(predicate, index)
					if (!rule.supportsPreparation() || rule.observedPredicate !== predicate) return false
				}
				return true
			}
			for (index in 0 until predicate.limited.size + predicate.common.size) {
				val rule = ruleAt(predicate, index)
				if (!rule.supportsPreparation()) return false
				val source = rule.source
				if (source != null) {
					if (!trusted(source, diagnostic)) return false
				} else {
					val observed = rule.observedPredicate as? CombinedPredicate ?: return false
					if (!trusted(observed, diagnostic)) return false
				}
			}
			return true
		}

		private fun trusted(source: StructurePredicate, diagnostic: Boolean): Boolean = when (source) {
			is PreparedPositionRule -> !diagnostic || source.supportsCompleteDiagnosis

			is RestrictedPredicate -> trusted(source.predicate, diagnostic)

			is ConcatenatedPredicate -> {
				var supported = true
				for (child in source.predicates) {
					if (!trusted(child, diagnostic)) {
						supported = false
						break
					}
				}
				supported
			}

			is DataHatchOrPredicate -> trusted(source.alternative, diagnostic)

			is AnyPredicate, is AirPredicate, is BlockPredicate, is BlockStatePredicate, is BlockTagPredicate,
			is FluidPredicate, is FluidTagPredicate, is SnowPredicate, is FramePredicate, is AbilityPredicate,
			is HeatingCoilPredicate, is CleanroomFilterPredicate, is PowerSubstationBatteryPredicate,
			is CharcoalLogOrAirPredicate, is CleanroomDoorPredicate, is CleanroomInnerPredicate,
			is RotorHolderPredicate, is AutoRecipeAbilityPredicate, is AutoMaintenancePredicate,
			is ActiveTransformerHatchPredicate, is CentralMonitorComponentPredicate, is CleanroomBasePredicate,
			-> true

			else -> false
		}

		fun dependencies(predicate: PatternPredicate, state: MultiblockState, positions: LongSet) {
			positions.add(state.pos.asLong())
			if (predicate is CombinedPredicate) for (branch in predicate.preparedBranches) dependencies(branch, state, positions)
			for (index in 0 until predicate.limited.size + predicate.common.size) {
				val rule = ruleAt(predicate, index)
				rule.source?.let { dependencies(it, state, positions) }
				val observed = rule.observedPredicate as? CombinedPredicate
				if (observed != null) for (branch in observed.preparedBranches) dependencies(branch, state, positions)
			}
		}

		private fun dependencies(source: StructurePredicate, state: MultiblockState, positions: LongSet) {
			when (source) {
				is RestrictedPredicate -> dependencies(source.predicate, state, positions)
				is AlternativePredicate -> for (child in source.alternatives(state)) dependencies(child, state, positions)
				else -> source.collectDependencies(state, positions)
			}
		}
	}
}

private class NativePreparedNode(val pattern: PatternPredicate? = null, val source: StructurePredicate? = null, var observation: PatternObservation? = null, var alternatives: ObjectList<StructurePredicate> = ObjectLists.emptyList(), var node: PreparedNode? = null)
