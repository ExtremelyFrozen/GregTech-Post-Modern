package com.gregtechceu.gtceu.api.multiblock.structurepredicate

import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo
import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.common.registry.GTRegistration.REGISTRATE

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.tags.TagKey
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.Fluid
import net.neoforged.neoforge.common.util.Lazy

import com.mojang.serialization.MapCodec
import it.unimi.dsi.fastutil.objects.ObjectImmutableList
import it.unimi.dsi.fastutil.objects.ObjectList

import java.util.stream.StreamSupport

class FluidTagPredicate(val tag: TagKey<Fluid>) : StructurePredicate {
	override fun hasPositionDependencies(): Boolean = true
	override fun canReuseObservation(): Boolean = true
	private val fluids = Lazy.of {
		StreamSupport.stream(BuiltInRegistries.FLUID.getTagOrEmpty(tag).spliterator(), false)
			.map { it.value() }.collect(ObjectImmutableList.toList())
	}
	override fun type(): StructurePredicateType<*> = TYPE
	override fun observeCandidate(candidateState: BlockState): PatternObservation = PatternObservation.of(candidateState.fluidState.`is`(tag))
	override fun candidates(): ObjectList<MultiblockBlockInfo> = fluids.get().stream().map(FluidPredicate::blockInfoFromFluid).collect(ObjectImmutableList.toList())
	override fun blockCandidates(): ObjectList<Block> = fluids.get().stream().map(FluidPredicate::blockStateFromFluid).map(BlockState::getBlock).distinct().collect(ObjectImmutableList.toList())
	override fun test(multiblockState: MultiblockState, mutateCount: Boolean): Boolean = observeCandidate(multiblockState.blockState).matched
	override fun equals(other: Any?): Boolean = other === this || (other is FluidTagPredicate && tag == other.tag)
	override fun hashCode(): Int = tag.hashCode()
	override fun toString(): String = "FluidTagPredicate[tag=$tag]"

	companion object {
		val TYPE: StructurePredicateType<FluidTagPredicate> by lazy { REGISTRATE.structurePredicate("fluid_tag", CODEC) }
		val CODEC: MapCodec<FluidTagPredicate> = TagKey.codec(Registries.FLUID).fieldOf("tag").xmap(::FluidTagPredicate, FluidTagPredicate::tag)
	}
}
