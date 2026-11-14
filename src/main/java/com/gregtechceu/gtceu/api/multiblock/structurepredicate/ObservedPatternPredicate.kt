package com.gregtechceu.gtceu.api.multiblock.structurepredicate

import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition
import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo
import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.api.multiblock.error.StructureIssue
import com.gregtechceu.gtceu.api.multiblock.pattern.match.PatternDiagnostic

import net.minecraft.network.chat.Component
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState

import it.unimi.dsi.fastutil.objects.ObjectList

import java.util.function.BiFunction
import java.util.function.Function
import java.util.function.Supplier

/** Read-only canonical predicate; the legacy test method publishes only a successful observation. */
open class ObservedPatternPredicate(
	private val observer: Function<MultiblockState, PatternObservation>,
	private val candidateBlocks: Supplier<Array<Block>>,
	private val kind: String = "",
	private val candidateObservation: Function<BlockState, PatternObservation>? = null,
	states: Supplier<ObjectList<BlockState>>? = null,
	private val diagnostic: BiFunction<MultiblockState, PatternDiagnostic, PatternDiagnostic>? = null,
) : PatternPredicate(
	{ state -> observer.apply(state).matched },
	Supplier {
		states?.get()?.firstOrNull()?.let { MultiblockBlockInfo(it) }
			?: candidateBlocks.get().firstOrNull()?.let { MultiblockBlockInfo(it) }
			?: MultiblockBlockInfo.EMPTY
	},
	candidateBlocks,
) {
	init {
		common.first().candidateObserver = Function { observeCandidate(it) }
		common.first().positionObserver = Function { observer.apply(it).withAliases(facts) }
		common.first().observedPredicate = this
		common.first().previewStates = states
		common.first().sealPreparedCallbacks()
	}
	override fun observeCandidate(state: BlockState): PatternObservation = (candidateObservation?.apply(state) ?: PatternObservation.of(candidateBlocks.get().contains(state.block))).withAliases(facts)

	/** Definition data is bound when this canonical predicate is compiled. */
	override fun observeCandidate(state: BlockState, definition: MultiblockMachineDefinition): PatternObservation = observeCandidate(state)
	override fun getPreviewCandidates(definition: MultiblockMachineDefinition): ObjectList<PatternPreviewCandidate> = getPreviewCandidates()
	override fun isAny(): Boolean = kind == "any" || super.isAny()
	override fun description(state: MultiblockState, excluded: Block?): Component? = if (isAny()) null else super.description(state, excluded)
	override fun isAir(): Boolean = kind == "air" || super.isAir()
	override fun addCache(): Boolean = kind != "any" && super.addCache()
	fun observe(state: MultiblockState): PatternObservation = if (canAcceptObserved(state)) observer.apply(state).withAliases(facts) else PatternObservation.of(false)
	override fun diagnose(state: MultiblockState, failure: PatternDiagnostic): PatternDiagnostic = diagnostic?.apply(state, failure) ?: failure.copy(issue = StructureIssue.mismatch(failure, state.predicate.description(state, failure.actual?.block)))

	/** Applies legacy limit/IO metadata only after a pure observation has succeeded. */
	open fun accept(state: MultiblockState, observation: PatternObservation): Boolean = observation.matched && acceptObservation(state, observation) && acceptObserved(state)

	/** Domain commit without this wrapper's metadata, also used by its flattened OR/copy rule. */
	internal open fun acceptObservation(state: MultiblockState, observation: PatternObservation): Boolean = true

	override fun observePosition(state: MultiblockState): PatternObservation = observe(state)
	override fun acceptPosition(state: MultiblockState, observation: PatternObservation): Boolean = accept(state, observation)

	override fun test(blockWorldState: MultiblockState): Boolean {
		blockWorldState.io = IO.BOTH
		return accept(blockWorldState, observe(blockWorldState))
	}
}
