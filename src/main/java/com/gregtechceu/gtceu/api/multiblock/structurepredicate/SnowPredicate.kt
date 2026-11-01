package com.gregtechceu.gtceu.api.multiblock.structurepredicate

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo
import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.utils.GTUtil

import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState

import com.mojang.serialization.MapCodec
import it.unimi.dsi.fastutil.objects.ObjectList

enum class SnowPredicate : StructurePredicate {
	INSTANCE,
	;

	override fun type(): StructurePredicateType<*> = TYPE
	override fun observeCandidate(state: BlockState): PatternObservation = PatternObservation.of(GTUtil.isBlockSnow(state))
	override fun test(multiblockState: MultiblockState, mutateCount: Boolean): Boolean = GTUtil.isBlockSnow(multiblockState.blockState)
	override fun candidates(): ObjectList<MultiblockBlockInfo> = ObjectList.of(MultiblockBlockInfo.fromBlockState(Blocks.SNOW_BLOCK.defaultBlockState()))
	override fun blockCandidates(): ObjectList<Block> = ObjectList.of(Blocks.SNOW_BLOCK)

	companion object {
		val TYPE: StructurePredicateType<SnowPredicate> by lazy { StructurePredicateType.register(GTCEu.id("snow"), CODEC) }

		@JvmField
		val CODEC: MapCodec<SnowPredicate> = MapCodec.unit(INSTANCE)
	}
}
