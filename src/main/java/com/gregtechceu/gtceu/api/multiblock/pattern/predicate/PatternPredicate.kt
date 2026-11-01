package com.gregtechceu.gtceu.api.multiblock.pattern.predicate

import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition
import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo
import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.api.multiblock.pattern.match.PatternDiagnostic
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.StructurePredicate
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection

import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
import it.unimi.dsi.fastutil.objects.ObjectList
import it.unimi.dsi.fastutil.objects.ObjectLists

import java.util.function.Function
import java.util.function.Predicate
import java.util.function.Supplier

/** Composite runtime predicate used by the matcher and structure planner. */
open class PatternPredicate {
	/** Builds the failure shown at this position; extensions retain its path and coordinates when adding detail. */
	open fun diagnose(failure: PatternDiagnostic): PatternDiagnostic = failure

	/** Observes a candidate without constructing block entities, mutating counters or consulting a world. */
	open fun observeCandidate(state: BlockState): PatternObservation {
		observeRulesCandidate(state)?.let { return it }
		for (predicate in structurePatternPredicates) {
			val observation = predicate.observeCandidate(state)
			if (observation.matched) return observation.withAliases(facts)
		}
		return PatternObservation.of(false)
	}

	/** Definition-aware choices carry their own observer, including dynamically injected third-party candidates. */
	open fun observeCandidate(state: BlockState, definition: MultiblockMachineDefinition): PatternObservation {
		observeRulesCandidate(state)?.let { return it }
		for (predicate in structurePatternPredicates) {
			for (choice in predicate.previewChoices(definition)) {
				val observation = choice.observeCandidate(state)
				if (observation.matched) return observation.withAliases(facts)
			}
		}
		return PatternObservation.of(false)
	}

	private fun observeRulesCandidate(state: BlockState): PatternObservation? {
		for (rule in limited.asSequence() + common.asSequence()) {
			val observation = rule.observeCandidate(state)
			if (observation.matched) return observation.withAliases(facts)
		}
		return null
	}

	/** Called after legacy count checks to retain the selected semantic branch's observation. */
	fun observeAccepted(state: MultiblockState): PatternObservation {
		for (rule in limited.asSequence() + common.asSequence()) {
			if ((rule.maxCount >= 0 && state.globalCount.getInt(rule) > rule.maxCount) || (rule.maxLayerCount >= 0 && state.layerCount.getInt(rule) > rule.maxLayerCount)) continue
			val observation = rule.positionObserver?.apply(state) ?: continue
			if (observation.matched) return observation.withAliases(facts)
		}
		val definition = state.lastController?.definition ?: state.controller?.definition
		return if (definition == null) observeCandidate(state.blockState) else observeCandidate(state.blockState, definition)
	}

	open fun getPreviewCandidates(): ObjectList<PatternPreviewCandidate> {
		val states = ObjectLinkedOpenHashSet<BlockState>()
		for (rule in limited.asSequence() + common.asSequence()) {
			states.addAll(rule.candidateStates())
		}
		structurePatternPredicates.forEach { states.addAll(it.candidateStates()) }
		if (isAny() || isAir() || hasAir()) states.add(Blocks.AIR.defaultBlockState())
		val result = ObjectArrayList<PatternPreviewCandidate>()
		states.forEach { state ->
			val observation = observeCandidate(state)
			if (observation.matched) result.add(PatternPreviewCandidate(MultiblockBlockInfo(state), observation))
		}
		return ObjectLists.unmodifiable(result)
	}

	open fun getPreviewCandidates(definition: MultiblockMachineDefinition): ObjectList<PatternPreviewCandidate> {
		val result = ObjectArrayList<PatternPreviewCandidate>()
		for (candidate in getPreviewCandidates()) {
			val observation = observeCandidate(candidate.blockInfo.blockState, definition)
			if (observation.matched) result.add(PatternPreviewCandidate(candidate.blockInfo, observation))
		}
		for (predicate in structurePatternPredicates) {
			for (choice in predicate.previewChoices(definition)) {
				for (info in choice.candidates()) {
					val observation = observeCandidate(info.blockState, definition)
					if (observation.matched) result.add(PatternPreviewCandidate(info, observation))
				}
			}
		}
		return ObjectLists.unmodifiable(result)
	}

	@JvmField
	var common: ObjectList<PredicateRule> = ObjectArrayList()

	@JvmField
	var limited: ObjectList<PredicateRule> = ObjectArrayList()

	@JvmField
	var structurePatternPredicates: ObjectList<StructurePredicate> = ObjectArrayList()

	@JvmField
	var facts: ObjectList<String> = ObjectArrayList()

	@JvmField
	var direction: Function<MultiblockState, Direction?> = Function { null }

	@JvmField
	var fixedDirection: Direction? = null

	@JvmField
	var relativeDirection: RelativeDirection? = null

	constructor()

	constructor(
		predicate: Predicate<MultiblockState>,
		blockInfo: Supplier<MultiblockBlockInfo>,
		candidates: Supplier<Array<Block>>?,
	) : this() {
		common.add(PredicateRule(predicate, blockInfo, candidates))
	}

	constructor(
		predicate: Predicate<MultiblockState>,
		candidates: Supplier<Array<MultiblockBlockInfo>>,
	) : this(
		predicate,
		Supplier {
			val infos = candidates.get()
			if (infos.isEmpty()) MultiblockBlockInfo.EMPTY else infos[0]
		},
		Supplier { candidates.get().map { it.blockState.block }.toTypedArray() },
	)

	constructor(simplePredicate: PredicateRule) : this() {
		if (simplePredicate.minCount != -1 || simplePredicate.maxCount != -1) {
			limited.add(simplePredicate)
		} else {
			common.add(simplePredicate)
		}
	}

	constructor(structurePredicate: StructurePredicate) : this() {
		structurePatternPredicates.add(structurePredicate)
	}

	constructor(predicate: PatternPredicate) : this() {
		common.addAll(predicate.common)
		limited.addAll(predicate.limited)
		structurePatternPredicates.addAll(predicate.structurePatternPredicates)
		facts.addAll(predicate.facts)
		direction = predicate.direction
		fixedDirection = predicate.fixedDirection
		relativeDirection = predicate.relativeDirection
	}

	fun sort(): PatternPredicate {
		limited.sortBy { it.minCount }
		return this
	}

	fun setDirection(direction: Direction): PatternPredicate {
		fixedDirection = direction
		relativeDirection = null
		this.direction = Function { direction }
		return this
	}

	fun setRelativeDirection(direction: RelativeDirection): PatternPredicate {
		fixedDirection = null
		relativeDirection = direction
		return this
	}

	fun getDirection(state: MultiblockState, frontFacing: Direction, upwardsFacing: Direction, isFlipped: Boolean): Direction? {
		relativeDirection?.let { return it.getRelative(frontFacing, upwardsFacing, isFlipped) }
		fixedDirection?.let { return it }
		return direction.apply(state)
	}

	fun getPreviewDirection(): Direction? = relativeDirection?.global ?: fixedDirection

	/** Add tooltips for candidates. They are shown in JEI Pages. */
	fun addTooltips(vararg tips: Component): PatternPredicate {
		if (tips.isEmpty()) return this
		common.forEach { predicate ->
			if (predicate.candidates == null) return@forEach
			if (predicate.toolTips == null) predicate.toolTips = ObjectArrayList()
			predicate.toolTips!!.addAll(tips.asList())
		}
		limited.forEach { predicate ->
			if (predicate.candidates == null) return@forEach
			if (predicate.toolTips == null) predicate.toolTips = ObjectArrayList()
			predicate.toolTips!!.addAll(tips.asList())
		}
		return this
	}

	fun setMinGlobalLimited(min: Int): PatternPredicate {
		limited.addAll(common)
		common.clear()
		limited.forEach { it.minCount = min }
		return this
	}

	fun setMinGlobalLimited(min: Int, previewCount: Int): PatternPredicate = setMinGlobalLimited(min).setPreviewCount(previewCount)

	fun setMaxGlobalLimited(max: Int): PatternPredicate {
		limited.addAll(common)
		common.clear()
		limited.forEach { it.maxCount = max }
		return this
	}

	fun setMaxGlobalLimited(max: Int, previewCount: Int): PatternPredicate = setMaxGlobalLimited(max).setPreviewCount(previewCount)

	fun setMinLayerLimited(min: Int): PatternPredicate {
		limited.addAll(common)
		common.clear()
		limited.forEach { it.minLayerCount = min }
		return this
	}

	fun setMinLayerLimited(min: Int, previewCount: Int): PatternPredicate = setMinLayerLimited(min).setPreviewCount(previewCount)

	fun setMaxLayerLimited(max: Int): PatternPredicate {
		limited.addAll(common)
		common.clear()
		limited.forEach { it.maxLayerCount = max }
		return this
	}

	fun setMaxLayerLimited(max: Int, previewCount: Int): PatternPredicate = setMaxLayerLimited(max).setPreviewCount(previewCount)

	fun setExactLimit(limit: Int): PatternPredicate = setMinGlobalLimited(limit).setMaxGlobalLimited(limit)

	fun setPreviewCount(count: Int): PatternPredicate {
		common.forEach { it.previewCount = count }
		limited.forEach { it.previewCount = count }
		return this
	}

	fun disableRenderFormed(): PatternPredicate {
		common.forEach { it.disableRenderFormed = true }
		limited.forEach { it.disableRenderFormed = true }
		return this
	}

	fun setIO(io: IO): PatternPredicate {
		common.forEach { it.io = io }
		limited.forEach { it.io = io }
		return this
	}

	fun setSlotName(slotName: String): PatternPredicate {
		common.forEach { it.slotName = slotName }
		limited.forEach { it.slotName = slotName }
		return this
	}

	open fun test(blockWorldState: MultiblockState): Boolean {
		blockWorldState.io = IO.BOTH
		var matched = false
		limited.forEach { if (it.testLimited(blockWorldState)) matched = true }
		if (!matched) matched = common.any { it.test(blockWorldState) }
		if (!matched) matched = structurePatternPredicates.any { it.test(blockWorldState, true) }
		if (matched) {
			blockWorldState.setError(null)
			facts.forEach(blockWorldState.facts::addFact)
		}
		return matched
	}

	fun withFacts(additionalFacts: List<String>): PatternPredicate {
		facts.addAll(additionalFacts)
		return this
	}

	fun or(other: PatternPredicate?): PatternPredicate {
		if (other == null) return this
		return PatternPredicate(this).also {
			it.common.addAll(other.common)
			it.limited.addAll(other.limited)
			it.structurePatternPredicates.addAll(other.structurePatternPredicates)
		}
	}

	open fun isAny(): Boolean = (
		common.size == 1 && limited.isEmpty() && structurePatternPredicates.isEmpty() &&
			common.first() === PredicateRule.any
		) ||
		(
			common.isEmpty() && limited.isEmpty() && structurePatternPredicates.size == 1 &&
				structurePatternPredicates.first().isAny()
			)

	open fun addCache(): Boolean = common.any { it.addCache() } || limited.any { it.addCache() } ||
		structurePatternPredicates.any { it.addCache() }

	open fun isAir(): Boolean = (
		common.size == 1 && limited.isEmpty() && structurePatternPredicates.isEmpty() &&
			common.first() === PredicateRule.air
		) ||
		(
			common.isEmpty() && limited.isEmpty() && structurePatternPredicates.size == 1 &&
				structurePatternPredicates.first().isAir()
			)

	fun isSingle(): Boolean = !isAny() && !isAir() && common.size + limited.size + structurePatternPredicates.size == 1

	fun hasAir(): Boolean = PredicateRule.air in common || structurePatternPredicates.any { it.hasAir() }
}
