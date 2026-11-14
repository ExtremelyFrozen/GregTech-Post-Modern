package com.gregtechceu.gtceu.api.multiblock.structurepredicate

import com.gregtechceu.gtceu.api.GTCEuAPI
import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.api.multiblock.autobuild.BlockCategories
import com.gregtechceu.gtceu.api.multiblock.error.PatternStringError
import com.gregtechceu.gtceu.api.multiblock.error.StructureIssue
import com.gregtechceu.gtceu.api.multiblock.pattern.match.PatternDiagnostic
import com.gregtechceu.gtceu.common.registry.GTRegistration.REGISTRATE

import net.minecraft.world.level.block.state.BlockState

import com.mojang.serialization.MapCodec
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps
import it.unimi.dsi.fastutil.objects.ObjectList

enum class CleanroomFilterPredicate : CategoryPredicate {
	INSTANCE,
	;

	override fun hasPositionDependencies(): Boolean = true

	override val category: String = BlockCategories.CLEANROOM_FILTERS
	override fun type(): StructurePredicateType<*> = TYPE
	override fun diagnose(state: MultiblockState, failure: PatternDiagnostic): PatternDiagnostic {
		val actual = observeCandidate(state.blockState)
		return failure.copy(issue = if (actual.matched && state.facts.get<Any>("FilterType") != actual.properties["FilterType"]) StructureIssue.mixed(failure, StructureIssue.text("filters")) else StructureIssue.mismatch(failure, state.predicate.description(state, failure.actual?.block)))
	}

	override fun observeCandidate(candidateState: BlockState): PatternObservation {
		val stats = GTCEuAPI.CLEANROOM_FILTERS.entries.firstOrNull { it.value.get() == candidateState.block }?.key
			?: return PatternObservation.of(false)
		return PatternObservation.of(true, ObjectList.of("filter"), Object2ObjectMaps.singleton("FilterType", stats))
	}

	override fun observePosition(state: MultiblockState): PatternObservation {
		val observed = observeCandidate(state.blockState)
		if (!observed.matched) return observed
		val current = state.facts.get<Any>("FilterType")
		return if (current == null || current == observed.properties["FilterType"]) observed else PatternObservation.of(false)
	}
	override fun acceptPosition(state: MultiblockState, observation: PatternObservation): Boolean {
		if (!observation.matched) return false
		state.facts.getOrPut("FilterType", observation.properties["FilterType"]!!)
		return true
	}

	override fun test(multiblockState: MultiblockState, mutateCount: Boolean): Boolean {
		for ((stats, block) in GTCEuAPI.CLEANROOM_FILTERS.entries) {
			if (!multiblockState.blockState.`is`(block.get())) continue
			val currentFilter: Any = multiblockState.facts.getOrPut("FilterType", stats)
			if (currentFilter != stats) {
				multiblockState.setError(PatternStringError("gtpm.multiblock.pattern.error.filters"))
				return false
			}
			return true
		}
		return false
	}

	companion object {
		val TYPE: StructurePredicateType<CleanroomFilterPredicate> by lazy { REGISTRATE.structurePredicate("filter", CODEC) }

		@JvmField
		val CODEC: MapCodec<CleanroomFilterPredicate> = MapCodec.unit(INSTANCE)
	}
}
