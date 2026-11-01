package com.gregtechceu.gtceu.api.multiblock.structurepredicate

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo
import com.gregtechceu.gtceu.api.multiblock.MultiblockState

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.neoforged.neoforge.common.util.Lazy

import com.mojang.serialization.MapCodec
import it.unimi.dsi.fastutil.objects.ObjectImmutableList
import it.unimi.dsi.fastutil.objects.ObjectList

import java.util.function.Supplier

class BlockPredicate(blockIds: Supplier<ObjectList<ResourceLocation>>) : StructurePredicate {
	private val resolvedBlockIds = Lazy.of(blockIds)
	private val resolvedBlocks = Lazy.of {
		this.blockIds.stream().map(::resolveBlock).collect(ObjectImmutableList.toList())
	}

	constructor(blockIds: ObjectList<ResourceLocation>) : this(Supplier { blockIds })

	val blockIds: ObjectList<ResourceLocation> get() = resolvedBlockIds.get()
	val blocks: ObjectList<Block> get() = resolvedBlocks.get()

	override fun type(): StructurePredicateType<*> = TYPE
	override fun candidates(): ObjectList<MultiblockBlockInfo> = blocks.stream().map(::MultiblockBlockInfo).collect(ObjectImmutableList.toList())
	override fun blockCandidates(): ObjectList<Block> = blocks
	override fun test(multiblockState: MultiblockState, mutateCount: Boolean): Boolean = blocks.contains(multiblockState.blockState.block)
	override fun hasAir(): Boolean = blocks.contains(Blocks.AIR)
	override fun equals(other: Any?): Boolean = other === this || (other is BlockPredicate && blockIds == other.blockIds)
	override fun hashCode(): Int = blockIds.hashCode()
	override fun toString(): String = "BlockPredicate[blockIds=$blockIds]"

	companion object {
		val TYPE: StructurePredicateType<BlockPredicate> by lazy { StructurePredicateType.register(GTCEu.id("block"), CODEC) }
		val CODEC: MapCodec<BlockPredicate> = PredicateCodecs.oneOrMore(ResourceLocation.CODEC).fieldOf("ids").xmap(::BlockPredicate, BlockPredicate::blockIds)

		private fun resolveBlock(id: ResourceLocation): Block {
			val block = BuiltInRegistries.BLOCK.get(id)
			if (id != BuiltInRegistries.BLOCK.getKey(block)) {
				throw IllegalStateException("Unknown block id in structure block predicate: $id")
			}
			return block
		}
	}
}
