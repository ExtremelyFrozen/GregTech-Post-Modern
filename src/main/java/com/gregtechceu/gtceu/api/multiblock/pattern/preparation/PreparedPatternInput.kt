package com.gregtechceu.gtceu.api.multiblock.pattern.preparation

import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicBoolean

/** A detached instruction tree. None of these data objects retain game objects or executable callbacks. */
sealed interface PreparedNode {
	val id: Int

	data class Leaf(override val id: Int, val matched: Boolean, val facts: Array<String>, val properties: Array<PreparedProperty>, val consistency: String, val effect: Int, val aggregation: Array<String> = emptyArray()) : PreparedNode
	data class Pattern(override val id: Int, val rules: Array<PreparedRule>, val aliases: Array<String>) : PreparedNode
	data class Alternative(override val id: Int, val children: Array<PreparedNode>) : PreparedNode
	data class Restricted(override val id: Int, val counter: Int, val child: PreparedNode) : PreparedNode
	data class DataHatch(override val id: Int, val data: PreparedNode?, val alternative: PreparedNode) : PreparedNode
	data class Combined(override val id: Int, val selection: Int, val priorities: IntArray, val branches: Array<PreparedNode>, val aliases: Array<String> = emptyArray()) : PreparedNode
	data class Observed(override val id: Int, val expression: PreparedNode, val rules: Array<PreparedRule>, val aliases: Array<String>) : PreparedNode
}

data class PreparedProperty(val key: String, val value: Int, val reference: Int = value)

/** Counter identity is assigned on the world thread using each original map's equality semantics. */
data class PreparedCounter(val minimum: Int, val maximum: Int, val layerMinimum: Int, val layerMaximum: Int)

data class PreparedRule(val source: PreparedNode, val counter: Int, val limited: Boolean, val io: Int, val renderMask: Boolean, val slot: String?)

data class PreparedCell(val path: String, val repeatPath: String, val depth: Int, val height: Int, val width: Int, val position: Long, val token: String, val scopes: Array<String>, val root: PreparedNode, val loaded: Boolean, val directional: Boolean, val shareable: Boolean)

data class PreparedRequirement(val fact: String, val prefix: String, val minimum: Int, val maximum: Int, val message: String)

data class PreparedScope(val name: String, val cell: Int, val depth: Int, val height: Int, val width: Int, val position: Long, val repeatPath: String)

/** Owns only arrays, strings and detached instructions; evaluate may run on any worker thread. */
class PreparedPatternInput internal constructor(
	internal val cells: Array<PreparedCell>,
	internal val counters: Array<PreparedCounter>,
	internal val requirements: Array<PreparedRequirement>,
	internal val initialScopes: Array<PreparedScope>,
	internal val orientationValid: Boolean,
	internal val center: Long,
	internal val groups: PreparedGroupPlan? = null,
) {
	private val cancelled = AtomicBoolean()
	internal val layerRuns = IntArray(cells.size).also { runs ->
		var run = 0
		for (index in cells.indices) {
			if (index > 0 && cells[index - 1].depth != cells[index].depth) run++
			runs[index] = run
		}
	}

	/** Module presence uses position rules and upper bounds, without whole-structure minimums. */
	fun forModuleProbe(): PreparedPatternInput = PreparedPatternInput(
		cells,
		Array(counters.size) { counters[it].copy(minimum = -1, layerMinimum = -1) },
		emptyArray(),
		emptyArray(),
		orientationValid,
		center,
	)

	fun cancel() {
		cancelled.set(true)
	}

	internal fun checkpoint() {
		if (cancelled.get()) throw CancellationException("Prepared pattern evaluation cancelled")
	}

	fun evaluate(): PreparedPatternResult = if (groups == null) {
		PreparedPatternEvaluator(this).evaluate()
	} else {
		PreparedGroupEvaluator(this, groups).evaluate()
	}
}

/** Native position payloads are reconstructed from this detached selection only on the world thread. */
data class PreparedObservation(
	val node: Int,
	val matched: Boolean,
	val facts: Array<String> = emptyArray(),
	val properties: Array<PreparedProperty> = emptyArray(),
	val children: Array<PreparedObservation> = emptyArray(),
	val selected: IntArray = IntArray(0),
	val available: BooleanArray = BooleanArray(0),
)

/** Serializable diagnostics retain both coordinates and the original structure path. */
data class PreparedPatternProblem(val cell: Int, val path: String, val repeatPath: String, val depth: Int, val height: Int, val width: Int, val position: Long, val expected: String, val missingFact: String = "", val difference: Int = 0)

data class PreparedFactCount(val fact: String, val count: Int)
data class PreparedScopedCount(val scope: String, val fact: String, val count: Int)
data class PreparedAcceptedCell(val cell: Int, val observation: PreparedObservation, val io: Int)

/** Effects are ordered, so duplicate rules and partially accepted positions preserve their original contributions. */
data class PreparedEffect(val cell: Int, val leaf: Int = -1, val renderMask: Boolean = false, val slot: String? = null)

class PreparedPatternResult internal constructor(
	val matched: Boolean,
	val problems: Array<PreparedPatternProblem>,
	internal val accepted: Array<PreparedAcceptedCell>,
	internal val effects: Array<PreparedEffect>,
	internal val factCounts: Array<PreparedFactCount>,
	internal val scopes: Array<String>,
	internal val scopedCounts: Array<PreparedScopedCount>,
	internal val properties: Array<PreparedProperty>,
	internal val globalCounts: IntArray,
	internal val layerCounts: IntArray,
	internal val lastCell: Int,
	internal val lastIO: Int,
	internal val contributions: Array<PreparedGroupContribution>? = null,
)
