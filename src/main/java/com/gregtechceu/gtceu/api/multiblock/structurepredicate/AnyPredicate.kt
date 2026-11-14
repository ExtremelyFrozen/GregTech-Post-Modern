package com.gregtechceu.gtceu.api.multiblock.structurepredicate

import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.common.registry.GTRegistration.REGISTRATE

import net.minecraft.world.level.block.state.BlockState

import com.mojang.serialization.MapCodec

enum class AnyPredicate : StructurePredicate {
	INSTANCE,
	;

	override fun hasPositionDependencies(): Boolean = true
	override fun canReuseObservation(): Boolean = true

	override fun type(): StructurePredicateType<*> = TYPE
	override fun observeCandidate(candidateState: BlockState): PatternObservation = PatternObservation.of(true)
	override fun test(multiblockState: MultiblockState, mutateCount: Boolean): Boolean = true

	companion object {
		val TYPE: StructurePredicateType<AnyPredicate> by lazy { REGISTRATE.structurePredicate("any", CODEC) }

		@JvmField
		val CODEC: MapCodec<AnyPredicate> = MapCodec.unit(INSTANCE)
	}
}
