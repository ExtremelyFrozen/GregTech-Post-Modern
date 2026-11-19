package com.gregtechceu.gtceu.api.multiblock.pattern.preparation

import com.gregtechceu.gtceu.api.multiblock.MultiblockState

import net.minecraft.core.BlockPos

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap

/** One request's main-thread world reads, shared by failed candidates and module probes until cancellation. */
class PatternWorldCollection {
	private val positions = Long2ObjectOpenHashMap<CapturedPatternBlock>()

	internal fun read(state: MultiblockState, position: BlockPos): CapturedPatternBlock = positions.computeIfAbsent(position.asLong()) {
		val loaded = state.world.isLoaded(position)
		val block = if (loaded) state.world.getBlockState(position) else null
		CapturedPatternBlock(block, if (block?.hasBlockEntity() == true) state.world.getBlockEntity(position) else null)
	}
}
