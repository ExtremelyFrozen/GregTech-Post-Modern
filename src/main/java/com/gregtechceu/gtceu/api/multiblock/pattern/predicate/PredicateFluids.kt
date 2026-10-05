package com.gregtechceu.gtceu.api.multiblock.pattern.predicate

import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo

import net.minecraft.world.level.material.Fluid
import net.minecraft.world.level.material.Fluids

import java.util.function.Predicate
import java.util.function.Supplier

/** Matches the fluid state of one or more fluids. */
open class PredicateFluids(vararg initialFluids: Fluid?) : PredicateRule() {
	@JvmField
	protected var fluids: Array<Fluid> = initialFluids.filterNotNull().toTypedArray()

	init {
		buildPredicate()
	}

	open override fun buildPredicate(): PredicateRule {
		if (fluids.isEmpty()) fluids = arrayOf(Fluids.WATER)
		predicate = Predicate { state -> fluids.any { it == state.blockState.fluidState.type } }
		val blocks = fluids.map { it.defaultFluidState().createLegacyBlock().block }.toTypedArray()
		candidates = Supplier { blocks }
		val info = MultiblockBlockInfo.fromBlockState(fluids[0].defaultFluidState().createLegacyBlock())
		blockInfo = Supplier { info }
		return this
	}
}
