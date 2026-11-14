package com.gregtechceu.gtceu.api.multiblock.structurepredicate

import com.gregtechceu.gtceu.api.block.MetaMachineBlock
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility
import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.api.multiblock.autobuild.BlockCategories
import com.gregtechceu.gtceu.api.multiblock.error.StructureIssue
import com.gregtechceu.gtceu.api.multiblock.pattern.match.PatternDiagnostic
import com.gregtechceu.gtceu.common.machine.multiblock.part.RotorHolderPartMachine
import com.gregtechceu.gtceu.common.registry.GTRegistration.REGISTRATE

import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import it.unimi.dsi.fastutil.longs.LongSet
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectList

@JvmRecord
data class RotorHolderPredicate(val tier: Int) : CategoryPredicate {
	override fun hasPositionDependencies(): Boolean = true
	override fun canReuseObservation(): Boolean = true
	override fun collectDependencies(state: MultiblockState, positions: LongSet) {
		val rotor = state.blockEntity as? RotorHolderPartMachine ?: return
		positions.add(state.pos.relative(rotor.self().frontFacing).asLong())
	}
	override val category: String get() = BlockCategories.ROTOR_HOLDER
	override fun type(): StructurePredicateType<*> = TYPE
	override fun diagnose(state: MultiblockState, failure: PatternDiagnostic): PatternDiagnostic {
		val rotor = state.blockEntity as? RotorHolderPartMachine
		if (rotor == null || rotor.tier != tier) return failure.copy(issue = StructureIssue.mismatch(failure, state.predicate.description(state, failure.actual?.block)))
		val front = state.pos.relative(rotor.self().frontFacing)
		state.recordRead(front)
		val loaded = state.world.isLoaded(front)
		val dependency = failure.copy(world = front, actual = if (loaded) state.world.getBlockState(front) else null)
		return dependency.copy(issue = if (loaded) StructureIssue.forbidden(dependency) else StructureIssue.unloaded(dependency))
	}
	override fun observeCandidate(candidateState: BlockState): PatternObservation = PatternObservation.of(
		(candidateState.block as? MetaMachineBlock)?.definition?.tier == tier && PartAbility.ROTOR_HOLDER.isApplicable(candidateState.block),
	)
	override fun observePosition(state: MultiblockState): PatternObservation {
		val rotor = state.blockEntity as? RotorHolderPartMachine ?: return PatternObservation.of(false)
		val front = state.pos.relative(rotor.self().frontFacing)
		state.recordRead(front)
		state.addPosCache(front)
		return PatternObservation.of(rotor.tier == tier && state.world.isLoaded(front) && state.world.getBlockState(front).isAir)
	}
	override fun test(multiblockState: MultiblockState, mutateCount: Boolean): Boolean = observePosition(multiblockState).matched
	override fun blockCandidates(): ObjectList<Block> = ObjectArrayList<Block>().also { result ->
		BlockCategories.getBlocks(category).forEach { if (it is MetaMachineBlock && it.definition.tier == tier) result.add(it) }
	}

	companion object {
		val TYPE: StructurePredicateType<RotorHolderPredicate> by lazy { REGISTRATE.structurePredicate("rotor_holder", CODEC) }

		@JvmField
		val CODEC: MapCodec<RotorHolderPredicate> = RecordCodecBuilder.mapCodec { instance ->
			instance.group(Codec.INT.fieldOf("tier").forGetter(RotorHolderPredicate::tier)).apply(instance, ::RotorHolderPredicate)
		}
	}
}
