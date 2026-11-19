package com.gregtechceu.gtceu.api.multiblock.pattern.preparation

import com.gregtechceu.gtceu.api.multiblock.MultiblockState

import net.minecraft.core.BlockPos

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap

/** World-thread captures for one attempt. Confirmed captures remain read-only behind this attempt's changes. */
internal class CapturedPatternBlocks(private val confirmed: CapturedPatternBlocks? = null) {
	private val changes = Long2ObjectOpenHashMap<CapturedPatternBlock>()

	operator fun get(position: Long): CapturedPatternBlock? {
		var layer: CapturedPatternBlocks? = this
		while (layer != null) {
			val value = layer.changes.get(position)
			if (value != null) return value
			layer = layer.confirmed
		}
		return null
	}

	operator fun set(position: Long, block: CapturedPatternBlock) {
		changes.put(position, block)
	}

	fun registerReads(target: MultiblockState) {
		var layer: CapturedPatternBlocks? = this
		while (layer != null) {
			val positions = layer.changes.keys.iterator()
			while (positions.hasNext()) {
				val position = BlockPos.of(positions.nextLong())
				target.recordRead(position)
				target.addPosCache(position)
			}
			layer = layer.confirmed
		}
	}
}
