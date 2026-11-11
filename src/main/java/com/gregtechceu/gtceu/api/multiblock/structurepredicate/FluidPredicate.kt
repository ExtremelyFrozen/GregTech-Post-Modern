package com.gregtechceu.gtceu.api.multiblock.structurepredicate

import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo
import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.common.registry.GTRegistration.REGISTRATE

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.Fluid

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import it.unimi.dsi.fastutil.objects.ObjectImmutableList
import it.unimi.dsi.fastutil.objects.ObjectList

@JvmRecord
data class FluidPredicate(val fluids: ObjectList<Fluid>) : StructurePredicate {
	override fun type(): StructurePredicateType<*> = TYPE
	override fun observeCandidate(candidateState: BlockState): PatternObservation = PatternObservation.of(fluids.contains(candidateState.fluidState.type))
	override fun candidates(): ObjectList<MultiblockBlockInfo> = fluids.stream().map(::blockInfoFromFluid).collect(ObjectImmutableList.toList())
	override fun blockCandidates(): ObjectList<Block> = fluids.stream().map(::blockStateFromFluid).map(BlockState::getBlock).distinct().collect(ObjectImmutableList.toList())
	override fun test(multiblockState: MultiblockState, mutateCount: Boolean): Boolean = observeCandidate(multiblockState.blockState).matched

	companion object {
		val TYPE: StructurePredicateType<FluidPredicate> by lazy { REGISTRATE.structurePredicate("fluids", CODEC) }

		@JvmField
		val CODEC: MapCodec<FluidPredicate> = RecordCodecBuilder.mapCodec { instance ->
			instance.group(PredicateCodecs.oneOrMore(BuiltInRegistries.FLUID.byNameCodec()).fieldOf("fluids").forGetter(FluidPredicate::fluids))
				.apply(instance, ::FluidPredicate)
		}
		internal fun blockStateFromFluid(fluid: Fluid): BlockState = fluid.defaultFluidState().createLegacyBlock()
		internal fun blockInfoFromFluid(fluid: Fluid): MultiblockBlockInfo = MultiblockBlockInfo(blockStateFromFluid(fluid))
	}
}
