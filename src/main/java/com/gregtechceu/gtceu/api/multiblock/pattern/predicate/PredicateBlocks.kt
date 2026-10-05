package com.gregtechceu.gtceu.api.multiblock.pattern.predicate

import com.gregtechceu.gtceu.api.block.MetaMachineBlock
import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo

import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks

import it.unimi.dsi.fastutil.objects.ObjectArrayList

import java.util.function.Predicate
import java.util.function.Supplier

/** Matches one of a fixed set of blocks. */
open class PredicateBlocks(vararg initialBlocks: Block?) : PredicateRule() {
	@JvmField
	var blocks: Array<Block> = initialBlocks.filterNotNull().toTypedArray()

	init {
		buildPredicate()
	}

	open override fun buildPredicate(): PredicateRule {
		val filteredBlocks = ObjectArrayList<Block>(blocks.size)
		blocks.forEach { block -> if (block !== Blocks.AIR) filteredBlocks.add(block) }
		require(filteredBlocks.isNotEmpty()) { "Empty predicate: ${blocks.contentToString()}" }
		blocks = filteredBlocks.toTypedArray()
		val block = blocks[0]
		blockInfo = if (block is MetaMachineBlock) {
			Supplier { MultiblockBlockInfo.fromBlock(block) }
		} else {
			val info = MultiblockBlockInfo.fromBlock(block)
			Supplier { info }
		}
		predicate = Predicate { state -> blocks.any { it == state.blockState.block } }
		candidates = Supplier { blocks }
		return this
	}
}
