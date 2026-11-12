package com.gregtechceu.gtceu.api.multiblock.structurepredicate

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition
import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo
import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.api.multiblock.pattern.layout.PatternLayout

import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState

import it.unimi.dsi.fastutil.longs.LongSet
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectImmutableList
import it.unimi.dsi.fastutil.objects.ObjectList

/**
 * A domain-defined OR branch shares its actual child rules across runtime, metadata and preview.
 * Implementations keep child identities stable when they own counters. A null world state requests static
 * candidates; definition-dependent previews override previewAlternatives. Pure observations never test or mutate
 * a child, while the default world test visits every child so required zero-count branches remain represented.
 */
interface AlternativePredicate : StructurePredicate {
	/** Returns the actual ordered branches; null state requests the context-free candidate domain. */
	fun alternatives(state: MultiblockState?): ObjectList<StructurePredicate>

	override fun collectDependencies(state: MultiblockState, positions: LongSet) {
		positions.add(state.pos.asLong())
		alternatives(state).forEach { it.collectDependencies(state, positions) }
	}

	companion object {
		internal fun restorePreparedPosition(observation: PatternObservation, children: ObjectList<Pair<StructurePredicate, PatternObservation>>): PatternObservation = observation.withPositionMatch(Position(children))
	}

	/** Supplies definition-aware branches without creating or inspecting a world. */
	fun previewAlternatives(definition: MultiblockMachineDefinition, layout: PatternLayout?): ObjectList<StructurePredicate> = alternatives(null)

	override fun observeCandidate(candidateState: BlockState): PatternObservation {
		for (child in alternatives(null)) child.observeCandidate(candidateState).let { if (it.matched) return it }
		return PatternObservation.of(false)
	}
	private data class Position(val children: ObjectList<Pair<StructurePredicate, PatternObservation>>) : PatternObservation.PositionMatch

	override fun observePosition(state: MultiblockState): PatternObservation {
		val children = ObjectArrayList<Pair<StructurePredicate, PatternObservation>>()
		var first: PatternObservation? = null
		for (child in alternatives(state)) {
			val observed = child.observePosition(state)
			children.add(child to observed)
			if (first == null && observed.matched) first = observed
		}
		return (first ?: PatternObservation.of(false)).withPositionMatch(Position(children))
	}
	override fun acceptPosition(state: MultiblockState, observation: PatternObservation): Boolean {
		val position = observation.positionMatch as? Position ?: return false
		var matched = false
		for ((child, observed) in position.children) {
			if (!observation.matched && observed.matched) continue
			if (child.acceptPosition(state, observed)) matched = true
		}
		return matched
	}
	override fun test(multiblockState: MultiblockState, mutateCount: Boolean): Boolean {
		var matched = false
		for (child in alternatives(multiblockState)) if (child.test(multiblockState, mutateCount)) matched = true
		return matched
	}
	override fun candidateStates(): ObjectList<BlockState> = ObjectArrayList<BlockState>().also { result ->
		alternatives(null).forEach { result.addAll(it.candidateStates()) }
	}
	override fun candidates(): ObjectList<MultiblockBlockInfo> = alternatives(null).stream().flatMap { it.candidates().stream() }.collect(ObjectImmutableList.toList())
	override fun blockCandidates(): ObjectList<Block> = alternatives(null).stream().flatMap { it.blockCandidates().stream() }.collect(ObjectImmutableList.toList())
	override fun previewChoices(definition: MultiblockMachineDefinition): ObjectList<StructurePreviewChoice> = previewAlternatives(definition, null).stream().flatMap { it.previewChoices(definition).stream() }.collect(ObjectImmutableList.toList())
	override fun hasAir(): Boolean = alternatives(null).any { it.hasAir() }
}
