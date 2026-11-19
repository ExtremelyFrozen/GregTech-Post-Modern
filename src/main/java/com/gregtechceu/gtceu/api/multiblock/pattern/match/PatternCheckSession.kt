package com.gregtechceu.gtceu.api.multiblock.pattern.match

import com.gregtechceu.gtceu.api.multiblock.MultiblockState

import java.util.concurrent.Executor

/** A private candidate state is published only after all work and controller-specific checks have completed. */
class PatternCheckSession internal constructor(val state: MultiblockState, private val work: PatternWork<Boolean>, private val original: MultiblockState) : AutoCloseable {
	private val initialFacts = original.facts
	private val initialCache = original.matchCache
	private val version = original.changeVersion
	private val controller = state.lastController
	private val front = controller?.frontFacing
	private val up = controller?.upwardsFacing

	fun advance(executor: Executor): Boolean = work.advance(executor)

	/** Drives a new explicit check inline; it does not wait for or reuse an automatic task. */
	fun finish(): Boolean = work.finish()
	fun matched(): Boolean = work.result()
	fun isSourceCurrent(): Boolean = original.changeVersion == version && original.facts === initialFacts && original.matchCache === initialCache && controller?.frontFacing == front && controller?.upwardsFacing == up && state.isMatchCurrent

	override fun close() {
		work.cancel()
		state.closeReadTracking()
	}
}
