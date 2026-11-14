package com.gregtechceu.gtceu.api.multiblock.structurepredicate

import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.api.multiblock.error.StructureIssue
import com.gregtechceu.gtceu.api.multiblock.pattern.match.PatternDiagnostic
import com.gregtechceu.gtceu.common.registry.GTRegistration.REGISTRATE

import net.minecraft.world.level.block.state.BlockState

import com.mojang.serialization.MapCodec

enum class AirPredicate : StructurePredicate {
	INSTANCE,
	;

	override fun hasPositionDependencies(): Boolean = true
	override fun canReuseObservation(): Boolean = true

	override fun type(): StructurePredicateType<*> = TYPE
	override fun diagnose(state: MultiblockState, failure: PatternDiagnostic): PatternDiagnostic = failure.copy(issue = StructureIssue.forbidden(failure))
	override fun observeCandidate(candidateState: BlockState): PatternObservation = PatternObservation.of(candidateState.isAir)
	override fun test(multiblockState: MultiblockState, mutateCount: Boolean): Boolean = multiblockState.blockState.isAir
	override fun hasAir(): Boolean = true

	companion object {
		val TYPE: StructurePredicateType<AirPredicate> by lazy { REGISTRATE.structurePredicate("air", CODEC) }

		@JvmField
		val CODEC: MapCodec<AirPredicate> = MapCodec.unit(INSTANCE)
	}
}
