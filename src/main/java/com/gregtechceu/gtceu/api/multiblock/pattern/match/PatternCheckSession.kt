package com.gregtechceu.gtceu.api.multiblock.pattern.match

import com.gregtechceu.gtceu.api.multiblock.MultiblockState

/** A private candidate state is published only after all work and controller-specific checks have completed. */
class PatternCheckSession internal constructor(val state: MultiblockState, private val work: PatternWork<Boolean>, private val original: MultiblockState) {
	private val initialFacts = original.facts
	private val initialSnapshot = original.matchSnapshot
	private val controller = state.lastController
	private val front = controller?.frontFacing
	private val up = controller?.upwardsFacing

	fun advance(): Boolean = work.advance()
	fun matched(): Boolean = work.result()
	fun requiresRefresh(): Boolean = state.facts !== original.facts
	fun isSourceCurrent(): Boolean = original.facts === initialFacts && original.matchSnapshot === initialSnapshot && controller?.frontFacing == front && controller?.upwardsFacing == up
}
