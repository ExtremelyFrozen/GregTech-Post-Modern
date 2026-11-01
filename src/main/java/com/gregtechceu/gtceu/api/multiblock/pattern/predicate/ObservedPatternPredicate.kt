package com.gregtechceu.gtceu.api.multiblock.pattern.predicate

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition
import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo
import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.api.multiblock.pattern.match.PatternDiagnostic

import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState

import it.unimi.dsi.fastutil.objects.ObjectList

import java.util.function.Function
import java.util.function.Supplier

/** Read-only canonical predicate; the legacy test method publishes only a successful observation. */
class ObservedPatternPredicate(
	private val observer: Function<MultiblockState, PatternObservation>,
	private val candidateBlocks: Supplier<Array<Block>>,
	private val kind: String = "",
	private val candidateObservation: Function<BlockState, PatternObservation>? = null,
	states: Supplier<ObjectList<BlockState>>? = null,
	private val diagnostic: Function<PatternDiagnostic, PatternDiagnostic> = Function.identity(),
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
		common.first().positionObserver = Function { observe(it) }
		common.first().previewStates = states
	}
	override fun observeCandidate(state: BlockState): PatternObservation = (candidateObservation?.apply(state) ?: PatternObservation.of(candidateBlocks.get().contains(state.block))).withAliases(facts)

	/** Definition data is bound when this canonical predicate is compiled. */
	override fun observeCandidate(state: BlockState, definition: MultiblockMachineDefinition): PatternObservation = observeCandidate(state)
	override fun getPreviewCandidates(definition: MultiblockMachineDefinition): ObjectList<PatternPreviewCandidate> = getPreviewCandidates()
	override fun isAny(): Boolean = kind == "any" || super.isAny()
	override fun isAir(): Boolean = kind == "air" || super.isAir()
	override fun addCache(): Boolean = kind != "any" && super.addCache()
	fun observe(state: MultiblockState): PatternObservation = observer.apply(state).withAliases(facts)
	override fun diagnose(failure: PatternDiagnostic): PatternDiagnostic = diagnostic.apply(failure)

	/** Applies legacy limit/IO metadata only after a pure observation has succeeded. */
	fun accept(state: MultiblockState, observation: PatternObservation): Boolean = observation.matched && super.test(state)

	override fun test(blockWorldState: MultiblockState): Boolean = accept(blockWorldState, observe(blockWorldState))
}
