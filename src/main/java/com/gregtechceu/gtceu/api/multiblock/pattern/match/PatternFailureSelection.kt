package com.gregtechceu.gtceu.api.multiblock.pattern.match

import com.gregtechceu.gtceu.api.multiblock.MultiblockState

/** Keeps one failed interpretation intact; equal progress retains the original candidate order. */
internal class PatternFailureSelection {
	private var best: MultiblockState? = null

	fun include(state: MultiblockState) {
		val result = state.matchResult ?: return
		if (!result.matched && (best == null || result.matchedPositions > best!!.matchResult.matchedPositions)) best = state.captureMatch()
	}

	fun apply(state: MultiblockState) {
		best?.let(state::applyMatch)
	}
}
