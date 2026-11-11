package com.gregtechceu.gtceu.api.multiblock.structurepredicate

import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo
import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.common.registry.GTRegistration.REGISTRATE

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.Property

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import it.unimi.dsi.fastutil.objects.ObjectList

/** One block plus the properties actually constrained by this definition, shared by candidates and world matching. */
@JvmRecord
data class BlockStatePredicate(val block: Block, val properties: Map<String, String>) : StructurePredicate {
	private fun preview(): BlockState {
		var state = block.defaultBlockState()
		for ((name, value) in properties) {
			val property = block.stateDefinition.getProperty(name) ?: error("Unknown property $name")
			state = applyProperty(state, property, value)
		}
		return state
	}
	override fun type(): StructurePredicateType<*> = TYPE
	override fun candidateStates(): ObjectList<BlockState> = ObjectList.of(preview())
	override fun observeCandidate(candidateState: BlockState): PatternObservation = PatternObservation.of(
		candidateState.block == block && properties.all { (name, value) ->
			val property = block.stateDefinition.getProperty(name)
			property != null && candidateState.getValue(property).toString().equals(value, true)
		},
	)
	override fun candidates(): ObjectList<MultiblockBlockInfo> = ObjectList.of(MultiblockBlockInfo(preview()))
	override fun blockCandidates(): ObjectList<Block> = ObjectList.of(block)
	override fun test(multiblockState: MultiblockState, mutateCount: Boolean): Boolean = observeCandidate(multiblockState.blockState).matched
	override fun hasAir(): Boolean = observeCandidate(Blocks.AIR.defaultBlockState()).matched

	companion object {
		val TYPE: StructurePredicateType<BlockStatePredicate> by lazy { REGISTRATE.structurePredicate("block_state", CODEC) }
		val CODEC: MapCodec<BlockStatePredicate> = RecordCodecBuilder.mapCodec { instance ->
			instance.group(
				BuiltInRegistries.BLOCK.byNameCodec().fieldOf("block").forGetter(BlockStatePredicate::block),
				Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("properties", emptyMap()).forGetter(BlockStatePredicate::properties),
			).apply(instance, ::BlockStatePredicate)
		}
		private fun <T : Comparable<T>> applyProperty(state: BlockState, property: Property<T>, value: String): BlockState = state.setValue(property, property.getValue(value).orElseThrow { IllegalArgumentException("Unknown value '$value' for ${property.name}") })
	}
}
