package com.gregtechceu.gtceu.api.multiblock.structurepredicate

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition
import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo
import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.api.multiblock.error.StructureIssue
import com.gregtechceu.gtceu.api.multiblock.pattern.match.PatternDiagnostic
import com.gregtechceu.gtceu.api.sync_system.FieldCodecs

import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import it.unimi.dsi.fastutil.longs.LongSet
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectList
import it.unimi.dsi.fastutil.objects.ObjectLists

enum class RequirementScope { POSITION, ALTERNATIVE }

/**
 * A registered structure rule owns its codec, actual match semantics and representative observations.
 * Observations must be pure and safe for concurrent reads after registration; test runs on the world thread and
 * mutates counters or aggregation facts only when requested. Runtime wrappers add limits, IO and slot metadata
 * without replacing the rule's matching logic. Failed observations publish no facts or properties.
 */
interface StructurePredicate {
	/** Opt-in dependency contract. False retains full observation for custom or externally stateful rules. */
	fun hasPositionDependencies(): Boolean = false

	/** True only when observation does not read aggregate facts or count reservations. */
	fun canReuseObservation(): Boolean = false

	/** Registers every world position read by this rule, including positions outside the declared layout. */
	fun collectDependencies(state: MultiblockState, positions: LongSet) {
		positions.add(state.pos.asLong())
	}

	/** Observes a representative full state without creating block entities or consulting a world. */
	fun observeCandidate(candidateState: BlockState): PatternObservation = PatternObservation.of(blockCandidates().contains(candidateState.block))

	/** Observes world-dependent semantics without changing counters, caches, world state or shared facts. */
	fun observePosition(state: MultiblockState): PatternObservation = observeCandidate(state.blockState)

	/** Reads existing domain limits before a composite selects this branch; it never reserves counts. */
	fun canAcceptPosition(state: MultiblockState): Boolean = true

	/** Describes visible candidates in the owning machine's context; null means no concrete candidate remains. */
	fun description(state: MultiblockState, excluded: Block? = null): Component? = if (isAny()) null else StructureIssue.blocks(blockCandidates(), excluded)

	/** Main-thread failure guidance; alternative-only abilities are exposed by their unmet count restrictions. */
	fun requiredDescription(state: MultiblockState, excluded: Block?, scope: RequirementScope): Component? = description(state, excluded)

	/**
	 * Runs on the world thread at the failed cell, after count restoration and before publication. The returned
	 * diagnostic owns its explanation; describing a failure must not change world state, facts or counts.
	 */
	fun diagnose(state: MultiblockState, failure: PatternDiagnostic): PatternDiagnostic =
		failure.copy(issue = if (failure.missingFact.isEmpty()) StructureIssue.mismatch(failure, state.predicate.description(state, failure.actual?.block)) else StructureIssue.mixed(failure, Component.translatable(failure.missingFact)))

	/** Supplies representative full states; state-sensitive rules override this with their candidate observer. */
	fun candidateStates(): ObjectList<BlockState> = ObjectArrayList<BlockState>().also { states ->
		blockCandidates().forEach { states.add(it.defaultBlockState()) }
	}

	/** Applies source-owned orientation or IO metadata to one fresh runtime, without mutating the shared rule. */
	fun configureRuntime(predicate: PatternPredicate) {}

	fun type(): StructurePredicateType<*>
	fun candidates(): ObjectList<MultiblockBlockInfo> = ObjectList.of()
	fun blockCandidates(): ObjectList<Block> = ObjectList.of()

	/** Definition-aware choices preserve nested count restrictions and pure candidate observers. */
	fun previewChoices(definition: MultiblockMachineDefinition): ObjectList<StructurePreviewChoice> {
		val candidates = ObjectArrayList<MultiblockBlockInfo>()
		candidateStates().forEach { candidates.add(MultiblockBlockInfo(it)) }
		if (candidates.isNotEmpty()) {
			return ObjectLists.singleton(
				StructurePreviewChoice.unrestricted(candidates).withObserver(::observeCandidate),
			)
		}
		if (isAny() || isAir()) {
			return ObjectLists.singleton(
				StructurePreviewChoice.unrestricted(ObjectLists.singleton(MultiblockBlockInfo.EMPTY)).withObserver(::observeCandidate),
			)
		}
		return ObjectLists.emptyList()
	}

	/**
	 * Commits this rule's successful position observation on the same world thread before the position changes.
	 * Pure leaves need no second match. Stateful rules consume their own snapshot to apply counters or facts once;
	 * domain alternatives may also pass failed child observations to preserve required zero-count entries.
	 */
	fun acceptPosition(state: MultiblockState, observation: PatternObservation): Boolean = observation.matched

	/** Tests one real position. mutateCount controls counter and aggregation side effects; null states are invalid. */
	fun test(multiblockState: MultiblockState, mutateCount: Boolean): Boolean
	fun or(other: StructurePredicate): ConcatenatedPredicate = ConcatenatedPredicate.concat(this, other)
	fun isAny(): Boolean = this is AnyPredicate
	fun isAir(): Boolean = this is AirPredicate
	fun hasAir(): Boolean = false
	fun addCache(): Boolean = !isAny()

	companion object {
		@JvmField
		val CODEC: Codec<StructurePredicate> = typeCodec().dispatch("type", StructurePredicate::type) { it.codec }

		init {
			FieldCodecs.register(StructurePredicate::class.java, CODEC)
		}

		private fun typeCodec(): Codec<StructurePredicateType<*>> = ResourceLocation.CODEC.comapFlatMap(
			{ id ->
				StructurePredicateType.byId(id)?.let { DataResult.success(it) }
					?: DataResult.error { "Unknown structure predicate type: $id" }
			},
			{ it.id },
		)
	}
}
