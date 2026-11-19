package com.gregtechceu.gtceu.api.multiblock.structurepredicate

import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo
import com.gregtechceu.gtceu.api.multiblock.autobuild.BlockCategories

import net.minecraft.world.level.block.Block

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectList

/**
 * Shares only an ordered category's block candidates. Domain-specific observations, world conditions,
 * aggregation and diagnostics stay in the real predicate definition. Categories are initialized at common setup
 * and their arrays remain stable during concurrent read-only preview operations.
 */
interface CategoryPredicate : StructurePredicate {
	/** Stable classification key; this never encodes matching facts or effects. */
	val category: String
	override fun blockCandidates(): ObjectList<Block> = ObjectArrayList.wrap(BlockCategories.getBlocks(category))
	override fun candidates(): ObjectList<MultiblockBlockInfo> = ObjectArrayList<MultiblockBlockInfo>().also { result ->
		blockCandidates().forEach { result.add(MultiblockBlockInfo.fromBlock(it)) }
	}
}
