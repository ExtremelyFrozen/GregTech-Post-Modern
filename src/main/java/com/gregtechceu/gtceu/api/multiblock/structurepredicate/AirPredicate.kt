package com.gregtechceu.gtceu.api.multiblock.structurepredicate

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.PatternObservation

import net.minecraft.world.level.block.state.BlockState

import com.mojang.serialization.MapCodec

enum class AirPredicate : StructurePredicate {
	INSTANCE,
	;

	override fun type(): StructurePredicateType<*> = TYPE
	override fun observeCandidate(state: BlockState): PatternObservation = PatternObservation.of(state.isAir)
	override fun test(multiblockState: MultiblockState, mutateCount: Boolean): Boolean = multiblockState.blockState.isAir
	override fun hasAir(): Boolean = true

	companion object {
		val TYPE: StructurePredicateType<AirPredicate> by lazy { StructurePredicateType.register(GTCEu.id("air"), CODEC) }

		@JvmField
		val CODEC: MapCodec<AirPredicate> = MapCodec.unit(INSTANCE)
	}
}
