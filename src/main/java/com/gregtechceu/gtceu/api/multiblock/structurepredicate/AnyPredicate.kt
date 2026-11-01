package com.gregtechceu.gtceu.api.multiblock.structurepredicate

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.PatternObservation

import net.minecraft.world.level.block.state.BlockState

import com.mojang.serialization.MapCodec

enum class AnyPredicate : StructurePredicate {
	INSTANCE,
	;

	override fun type(): StructurePredicateType<*> = TYPE
	override fun observeCandidate(state: BlockState): PatternObservation = PatternObservation.of(true)
	override fun test(multiblockState: MultiblockState, mutateCount: Boolean): Boolean = true

	companion object {
		val TYPE: StructurePredicateType<AnyPredicate> by lazy { StructurePredicateType.register(GTCEu.id("any"), CODEC) }

		@JvmField
		val CODEC: MapCodec<AnyPredicate> = MapCodec.unit(INSTANCE)
	}
}
