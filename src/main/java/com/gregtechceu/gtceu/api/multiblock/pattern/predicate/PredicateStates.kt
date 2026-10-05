package com.gregtechceu.gtceu.api.multiblock.pattern.predicate

import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo

import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState

import java.util.function.Predicate
import java.util.function.Supplier

/** Matches a fixed set of block states. */
open class PredicateStates(vararg initialStates: BlockState?) : PredicateRule() {
	@JvmField
	var states: Array<BlockState> = initialStates.filterNotNull().toTypedArray()

	init {
		buildPredicate()
	}

	open override fun buildPredicate(): PredicateRule {
		if (states.isEmpty()) states = arrayOf(Blocks.BARRIER.defaultBlockState())
		predicate = Predicate { state -> states.any { it == state.blockState } }
		val blocks = states.map { it.block }.toTypedArray()
		candidates = Supplier { blocks }
		val info = MultiblockBlockInfo.fromBlockState(states[0])
		blockInfo = Supplier { info }
		return this
	}
}
