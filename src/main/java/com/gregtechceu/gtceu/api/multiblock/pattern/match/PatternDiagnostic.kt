package com.gregtechceu.gtceu.api.multiblock.pattern.match

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState

import org.jspecify.annotations.Nullable

/** A single actionable mismatch with both local and world coordinates. */
@JvmRecord
data class PatternDiagnostic(val path: String, val repeatPath: String, val local: PatternCoordinate, val world: BlockPos, val actual: @Nullable BlockState?, val expected: String, val missingFact: String, val difference: Int) {
	init {
		require(path.isNotBlank()) {
			"Pattern diagnostics require complete location and expectation data"
		}
	}
}
