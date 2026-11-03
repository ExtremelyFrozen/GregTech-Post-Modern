package com.gregtechceu.gtceu.api.multiblock.structurepredicate

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo
import com.gregtechceu.gtceu.api.multiblock.MultiblockState

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.tags.TagKey
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.common.util.Lazy

import com.mojang.serialization.MapCodec
import it.unimi.dsi.fastutil.objects.ObjectImmutableList
import it.unimi.dsi.fastutil.objects.ObjectList

import java.util.stream.StreamSupport

class BlockTagPredicate(val tag: TagKey<Block>) : StructurePredicate {
	private val blocks = Lazy.of {
		BuiltInRegistries.BLOCK.getTagOrEmpty(tag).spliterator().let {
			StreamSupport.stream(it, false).map { holder -> holder.value() }.collect(ObjectImmutableList.toList())
		}
	}
	override fun type(): StructurePredicateType<*> = TYPE
	override fun observeCandidate(candidateState: BlockState): PatternObservation = PatternObservation.of(candidateState.`is`(tag))
	override fun candidates(): ObjectList<MultiblockBlockInfo> = blocks.get().stream().map(::MultiblockBlockInfo).collect(ObjectImmutableList.toList())
	override fun blockCandidates(): ObjectList<Block> = blocks.get()
	override fun test(multiblockState: MultiblockState, mutateCount: Boolean): Boolean = observeCandidate(multiblockState.blockState).matched
	override fun hasAir(): Boolean = observeCandidate(Blocks.AIR.defaultBlockState()).matched
	override fun equals(other: Any?): Boolean = other === this || (other is BlockTagPredicate && tag == other.tag)
	override fun hashCode(): Int = tag.hashCode()
	override fun toString(): String = "BlockTagPredicate[tag=$tag]"

	companion object {
		val TYPE: StructurePredicateType<BlockTagPredicate> by lazy { StructurePredicateType.register(GTCEu.id("block_tag"), CODEC) }
		val CODEC: MapCodec<BlockTagPredicate> = TagKey.codec(Registries.BLOCK).fieldOf("tag").xmap(::BlockTagPredicate, BlockTagPredicate::tag)
	}
}
