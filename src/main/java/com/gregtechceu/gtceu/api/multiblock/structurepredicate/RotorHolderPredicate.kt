package com.gregtechceu.gtceu.api.multiblock.structurepredicate

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.block.MetaMachineBlock
import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility
import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.api.multiblock.autobuild.BlockCategories
import com.gregtechceu.gtceu.common.machine.multiblock.part.RotorHolderPartMachine

import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectList

@JvmRecord
data class RotorHolderPredicate(val tier: Int) : CategoryPredicate {
	override val category: String get() = BlockCategories.ROTOR_HOLDER
	override fun type(): StructurePredicateType<*> = TYPE
	override fun observeCandidate(candidateState: BlockState): PatternObservation = PatternObservation.of(
		(candidateState.block as? MetaMachineBlock)?.definition?.tier == tier && PartAbility.ROTOR_HOLDER.isApplicable(candidateState.block),
	)
	override fun observePosition(state: MultiblockState): PatternObservation {
		val rotor = MetaMachine.getMachine(state.world, state.pos) as? RotorHolderPartMachine ?: return PatternObservation.of(false)
		return PatternObservation.of(rotor.tier == tier && state.world.getBlockState(state.pos.relative(rotor.self().frontFacing)).isAir)
	}
	override fun test(multiblockState: MultiblockState, mutateCount: Boolean): Boolean = observePosition(multiblockState).matched
	override fun blockCandidates(): ObjectList<Block> = ObjectArrayList<Block>().also { result ->
		BlockCategories.getBlocks(category).forEach { if (it is MetaMachineBlock && it.definition.tier == tier) result.add(it) }
	}

	companion object {
		val TYPE: StructurePredicateType<RotorHolderPredicate> by lazy { StructurePredicateType.register(GTCEu.id("rotor_holder"), CODEC) }

		@JvmField
		val CODEC: MapCodec<RotorHolderPredicate> = RecordCodecBuilder.mapCodec { instance ->
			instance.group(Codec.INT.fieldOf("tier").forGetter(RotorHolderPredicate::tier)).apply(instance, ::RotorHolderPredicate)
		}
	}
}
