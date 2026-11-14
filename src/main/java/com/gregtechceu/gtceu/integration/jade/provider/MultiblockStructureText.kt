package com.gregtechceu.gtceu.integration.jade.provider

import com.gregtechceu.gtceu.api.block.MetaMachineBlock
import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.api.multiblock.error.PatternStringError
import com.gregtechceu.gtceu.api.multiblock.error.SinglePredicateError
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternConstraint
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.AbilityPredicate
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.PatternPredicate
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.PredicateRule
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.RestrictedPredicate
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.StructurePredicate

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.level.block.Block

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
import it.unimi.dsi.fastutil.objects.ObjectList
import it.unimi.dsi.fastutil.objects.ObjectLists

import kotlin.math.abs

/** Formats the last confirmed failure without starting another match or reading the world. */
internal object MultiblockStructureText {

	private enum class Kind(val key: String, val located: Boolean) {
		MISMATCH("mismatch", true),
		TOO_FEW("too_few", false),
		TOO_MANY("too_many", true),
		TOO_FEW_LAYER("too_few_layer", false),
		TOO_MANY_LAYER("too_many_layer", true),
		MIXED("mixed", true),
		SIZE("size", false),
		UNLOADED("unloaded", true),
		SHARED("shared", true),
		FORBIDDEN("forbidden", true),
		CONDITION("condition", true),
		;

		fun text(vararg args: Any): Component = Component.translatable("gtpm.structure.issue.$key", *args).withStyle(ChatFormatting.RED)
	}
	private enum class Rank { BLOCK, MACHINE, ABILITY }
	private data class Label(val text: Component, val rank: Rank)
	private data class CountFailure(val kind: Kind, val subject: Component, val limit: Int, val actual: Int)

	@JvmStatic
	fun lines(state: MultiblockState): ObjectList<Component> {
		if (state.error === MultiblockState.UNINIT_ERROR) return ObjectLists.singleton(text("unchecked").withStyle(ChatFormatting.GRAY))
		val issue = state.matchResult?.diagnostics?.firstOrNull()
			?: return ObjectLists.singleton((state.error as? PatternStringError)?.errorInfo?.copy()?.withStyle(ChatFormatting.RED) ?: Kind.CONDITION.text())
		val expected = issue.expected
		val count = when {
			state.error is SinglePredicateError -> singleCount(state, state.error as SinglePredicateError)
			expected == "layer minimum" -> count(state, minimum = true, layer = true)
			expected == "minimum count" -> count(state, minimum = true, layer = true) ?: count(state, minimum = true, layer = false)
			else -> count(state, minimum = false, layer = true) ?: count(state, minimum = false, layer = false)
		} ?: constraintCount(state, issue.missingFact, issue.difference, expected)
		val kind = count?.kind ?: when {
			expected.startsWith("loaded ") -> Kind.UNLOADED
			expected.startsWith("consistent ") -> Kind.MIXED
			expected.startsWith("measurement ") || expected.startsWith("layout ") -> Kind.SIZE
			expected == "multiblocked.pattern.error.share" -> Kind.SHARED
			expected == "orientation" || expected == "gtpm.structure.issue.orientation" -> Kind.CONDITION
			issue.path == "$" -> Kind.CONDITION
			state.predicate.isAir() -> Kind.FORBIDDEN
			else -> Kind.MISMATCH
		}
		val lines = ObjectArrayList<Component>()
		val subject = if (kind == Kind.MIXED) {
			when (val property = expected.removePrefix("consistent ")) {
				"CoilType" -> text("coils")
				"FilterType" -> text("filters")
				else -> Component.translatable(property)
			}
		} else {
			count?.subject ?: Component.empty()
		}
		lines.add(kind.text(subject.copy().withStyle(ChatFormatting.YELLOW)))
		if (kind.located) lines.add(text("position", number(issue.world.x, ChatFormatting.AQUA), number(issue.world.y, ChatFormatting.AQUA), number(issue.world.z, ChatFormatting.AQUA)).withStyle(ChatFormatting.GRAY))
		when {
			count != null -> lines.add((if (kind == Kind.TOO_FEW || kind == Kind.TOO_FEW_LAYER) text("at_least", number(count.limit, ChatFormatting.GREEN), number(count.actual, ChatFormatting.RED)) else text("at_most", number(count.limit, ChatFormatting.GREEN))).withStyle(ChatFormatting.GRAY))

			kind == Kind.MISMATCH -> label(state.predicate)?.let { lines.add(text("expected", it.copy().withStyle(ChatFormatting.GREEN)).withStyle(ChatFormatting.GRAY)) }

			kind == Kind.SIZE -> {
				val measurement = state.lastController.getPattern(state.structureName).getMeasurements().firstOrNull { issue.path == "$/measurement:${it.id}" }
				if (measurement == null) {
					lines.add(text("size.combination").withStyle(ChatFormatting.GRAY))
				} else if (expected.startsWith("measurement boundary")) {
					lines.add(text("size.unmeasured", Component.literal(measurement.parameter).withStyle(ChatFormatting.YELLOW), number(measurement.maximum, ChatFormatting.GREEN)).withStyle(ChatFormatting.GRAY))
				} else {
					val measured = abs(issue.local.depth - measurement.start.depth) + abs(issue.local.height - measurement.start.height) + abs(issue.local.width - measurement.start.width)
					lines.add(text("size.range", Component.literal(measurement.parameter).withStyle(ChatFormatting.YELLOW), number(measurement.minimum, ChatFormatting.GREEN), number(measurement.maximum, ChatFormatting.GREEN), number(measured, ChatFormatting.RED)).withStyle(ChatFormatting.GRAY))
				}
			}

			kind == Kind.CONDITION -> lines.add((if (expected == "orientation") text("orientation") else Component.translatable(expected)).withStyle(ChatFormatting.RED))
		}
		if (kind != Kind.UNLOADED && kind != Kind.SHARED) issue.actual?.let { lines.add(text("found", it.block.name.copy().withStyle(ChatFormatting.RED)).withStyle(ChatFormatting.GRAY)) }
		return lines
	}

	private fun singleCount(state: MultiblockState, error: SinglePredicateError): CountFailure {
		val rule = error.predicate
		return when (error.type) {
			0 -> CountFailure(Kind.TOO_MANY, label(rule).text, rule.maxCount, state.globalCount.getInt(rule))
			1 -> CountFailure(Kind.TOO_FEW, label(rule).text, rule.minCount, state.globalCount.getInt(rule))
			2 -> CountFailure(Kind.TOO_MANY_LAYER, label(rule).text, rule.maxLayerCount, state.layerCount.getInt(rule))
			else -> CountFailure(Kind.TOO_FEW_LAYER, label(rule).text, rule.minLayerCount, state.layerCount.getInt(rule))
		}
	}

	private fun count(state: MultiblockState, minimum: Boolean, layer: Boolean): CountFailure? {
		val kind = if (minimum) {
			if (layer) Kind.TOO_FEW_LAYER else Kind.TOO_FEW
		} else {
			if (layer) Kind.TOO_MANY_LAYER else Kind.TOO_MANY
		}
		for (entry in if (layer) state.layerCount.object2IntEntrySet() else state.globalCount.object2IntEntrySet()) {
			val rule = entry.key
			val actual = entry.intValue
			val limit = if (minimum) {
				if (layer) rule.minLayerCount else rule.minCount
			} else {
				if (layer) rule.maxLayerCount else rule.maxCount
			}
			if (limit >= 0 && if (minimum) actual < limit else actual > limit) return CountFailure(kind, label(rule).text, limit, actual)
		}
		for (entry in if (layer) state.structureLayerCount.object2IntEntrySet() else state.structureGlobalCount.object2IntEntrySet()) {
			val rule = entry.key
			val actual = entry.intValue
			if (rule !is RestrictedPredicate) continue
			val limit = (
				if (minimum) {
					if (layer) rule.minCountByLayer else rule.minCount
				} else {
					if (layer) rule.maxCountByLayer else rule.maxCount
				}
				).orElse(-1)
			if (limit >= 0 && if (minimum) actual < limit else actual > limit) return CountFailure(kind, label(rule.predicate), limit, actual)
		}
		return null
	}

	private fun constraintCount(state: MultiblockState, fact: String, difference: Int, expected: String): CountFailure? {
		if (fact.isEmpty() || difference == 0) return null
		for (constraint in state.lastController.getPattern(state.structureName).getSourceDefinition().constraints) {
			if (constraint is PatternConstraint.Count && constraint.fact == fact && constraint.message == expected) {
				val minimum = difference > 0
				val limit = if (minimum) constraint.minimum else constraint.maximum
				return CountFailure(if (minimum) Kind.TOO_FEW else Kind.TOO_MANY, Component.translatable(fact), limit, limit - difference)
			}
		}
		return null
	}

	private fun label(predicate: PatternPredicate): Component? {
		val labels = ObjectArrayList<Label>()
		fun add(rule: PredicateRule) {
			val label = label(rule)
			if (labels.none { it.text == label.text }) labels.add(label)
		}
		for (rule in predicate.common) add(rule)
		for (rule in predicate.limited) add(rule)
		if (labels.isEmpty) return null
		val result = Component.empty()
		var shown = 0
		for (rank in Rank.entries) {
			for ((name, order) in labels) {
				if (order != rank || shown == 3) continue
				if (shown++ > 0) result.append(" / ")
				result.append(name)
			}
		}
		return if (labels.size > 3) text("more", result, labels.size) else result
	}

	private fun label(rule: PredicateRule): Label {
		var source = rule.source
		while (source is RestrictedPredicate) source = source.predicate
		if (source is AbilityPredicate) return Label(label(source), Rank.ABILITY)
		val blocks = ObjectLinkedOpenHashSet<Block>()
		for (state in rule.candidateStates()) blocks.add(state.block)
		if (blocks.isEmpty()) return Label(text("matching"), Rank.BLOCK)
		val first = blocks.first()
		return Label(if (blocks.size == 1) first.name else text("variants", first.name, blocks.size), if (first is MetaMachineBlock) Rank.MACHINE else Rank.BLOCK)
	}

	private fun label(predicate: StructurePredicate): Component = when (predicate) {
		is RestrictedPredicate -> label(predicate.predicate)

		is AbilityPredicate -> Component.empty().also { result ->
			for ((index, ability) in predicate.abilities.withIndex()) {
				if (index > 0) result.append(" / ")
				result.append(Component.translatable("gtpm.multiblock.ability.${ability.name}"))
			}
		}

		else -> predicate.blockCandidates().let { blocks ->
			if (blocks.isEmpty()) {
				text("matching")
			} else if (blocks.size == 1) {
				blocks[0].name
			} else {
				text("variants", blocks[0].name, blocks.size)
			}
		}
	}

	private fun number(value: Int, color: ChatFormatting): Component = Component.literal(value.toString()).withStyle(color)
	private fun text(key: String, vararg args: Any) = Component.translatable("gtpm.structure.issue.$key", *args)
}
