package com.gregtechceu.gtceu.api.multiblock.structurepredicate

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo
import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.data.recipe.CustomTags

import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.DoorBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf

import com.mojang.serialization.MapCodec
import it.unimi.dsi.fastutil.objects.ObjectList

/** Full door states and runtime matching share the cleanroom-door tag. */
enum class CleanroomDoorPredicate : StructurePredicate {
	INSTANCE,
	;

	override fun type(): StructurePredicateType<*> = TYPE
	override fun observeCandidate(candidateState: BlockState): PatternObservation = PatternObservation.of(candidateState.`is`(CustomTags.CLEANROOM_DOORS))
	override fun test(multiblockState: MultiblockState, mutateCount: Boolean): Boolean = observeCandidate(multiblockState.blockState).matched
	override fun candidates(): ObjectList<MultiblockBlockInfo> = ObjectList.of(
		MultiblockBlockInfo(Blocks.IRON_DOOR.defaultBlockState()),
		MultiblockBlockInfo(Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER)),
	)
	override fun candidateStates(): ObjectList<BlockState> = ObjectList.of(
		Blocks.IRON_DOOR.defaultBlockState(),
		Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER),
	)
	override fun blockCandidates(): ObjectList<Block> = ObjectList.of(Blocks.IRON_DOOR)

	companion object {
		val TYPE: StructurePredicateType<CleanroomDoorPredicate> by lazy { StructurePredicateType.register(GTCEu.id("cleanroom_doors"), CODEC) }

		@JvmField
		val CODEC: MapCodec<CleanroomDoorPredicate> = MapCodec.unit(INSTANCE)
	}
}
