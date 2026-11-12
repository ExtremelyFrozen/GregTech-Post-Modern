package com.gregtechceu.gtceu.api.multiblock.diagnosis

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine
import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.api.multiblock.error.PatternStringError
import com.gregtechceu.gtceu.api.multiblock.error.SinglePredicateError
import com.gregtechceu.gtceu.api.multiblock.pattern.layout.PatternLayout
import com.gregtechceu.gtceu.api.multiblock.pattern.match.PatternDiagnostic
import com.gregtechceu.gtceu.api.multiblock.pattern.match.PatternMeasurements
import com.gregtechceu.gtceu.api.multiblock.pattern.match.PatternModuleSearch
import com.gregtechceu.gtceu.api.multiblock.pattern.match.PatternSelections
import com.gregtechceu.gtceu.api.multiblock.pattern.preparation.*
import com.gregtechceu.gtceu.api.multiblock.scheduling.FormationExecutors
import com.gregtechceu.gtceu.data.pattern.StructurePatternRegistry

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel

import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2IntMap
import it.unimi.dsi.fastutil.objects.Object2IntMaps
import it.unimi.dsi.fastutil.objects.Object2ObjectMap
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectList
import it.unimi.dsi.fastutil.objects.ObjectLists

import java.util.concurrent.CompletableFuture

/** One menu owns one read-only diagnosis. World reads, predicate observations and publication stay on its tick. */
class StructureDiagnosis @JvmOverloads constructor(
	private val controller: MultiblockControllerMachine,
	private val name: String,
	repetitions: IntArray? = null,
	selections: Object2ObjectMap<String, String> = Object2ObjectMaps.emptyMap(),
	flip: Boolean? = null,
	measurements: Object2IntMap<String> = Object2IntMaps.emptyMap(),
) : AutoCloseable {
	private val pattern = controller.getPattern(name)
	private val measurementsTrusted = pattern.getMeasurements().indices.all { PreparedRuleCompiler.trusted(pattern.measurementRule(it), diagnostic = true) }
	private val generation = StructurePatternRegistry.generation()
	private val collection = PatternWorldCollection()
	private val front = controller.frontFacing
	private val up = controller.upwardsFacing
	private val state = MultiblockState(controller.level, controller.blockPos, name).also {
		it.lastController = controller
		it.clean()
		it.beginReadTracking()
	}
	private data class Candidate(
		val counts: IntArray,
		val choices: Object2ObjectMap<String, String>,
		val front: Direction,
		val flip: Boolean,
		val layout: PatternLayout?,
		val measurements: Object2IntMap<String>,
		val source: String,
		val issues: Array<PreparedPatternProblem> = emptyArray(),
		val unsupported: Boolean = false,
		val probe: PatternModuleSearch.Step.Probe? = null,
	)
	private val candidates = candidates(repetitions, PatternSelections.of(selections), flip, measurements).iterator()
	private var pending: CompletableFuture<PreparedPatternResult>? = null
	private var pendingInput: PreparedPatternInput? = null
	private var current: Candidate? = null
	private var lastCandidateMatched = false
	private var moduleOutcome = PatternModuleSearch.Outcome(false)
	private var moduleProblems: Array<PreparedPatternProblem> = emptyArray()
	private var basis: Candidate? = null
	private var bestScore = -1
	private var bestGroups: Array<PreparedProblemGroup> = emptyArray()
	private var bestTruncated = false
	private var visited = 0
	private var attempts = 0
	private var incomplete = false
	private var truncated = false
	private var closed = false
	var report: DiagnosisReport? = null
		private set

	fun tick() {
		if (closed || report != null) return
		if (controller.isRemoved || state.controller !== controller || controller.frontFacing != front || controller.upwardsFacing != up || generation != StructurePatternRegistry.generation() || controller.getPattern(name) !== pattern || !state.isMatchCurrent) {
			finish(DiagnosisStatus.STALE)
			return
		}
		try {
			val calculation = pending
			if (calculation != null) {
				if (!calculation.isDone) return
				pending = null
				pendingInput = null
				val result = calculation.join()
				visited += result.evaluatedCells
				if (current?.probe != null) {
					moduleOutcome = PatternModuleSearch.Outcome(result.matched)
					moduleProblems = result.problems
				} else {
					lastCandidateMatched = result.matched
					if (result.matchedCells > bestScore || (result.matchedCells == bestScore && basis?.unsupported == true)) {
						bestScore = result.matchedCells
						bestGroups = result.problemGroups
						bestTruncated = result.problemsTruncated
						basis = current
					}
					if ((current?.layout?.cells?.size ?: 0) > result.evaluatedCells) truncated = true
				}
			}
			if (visited >= MAX_CELLS || attempts >= MAX_CANDIDATES || truncated) {
				truncated = true
				finish(DiagnosisStatus.COMPLETE)
				return
			}
			if (!candidates.hasNext()) {
				finish(DiagnosisStatus.COMPLETE)
				return
			}
			val candidate = candidates.next()
			current = candidate
			lastCandidateMatched = false
			attempts++
			if (candidate.issues.isNotEmpty() || candidate.unsupported) {
				if (candidate.unsupported) incomplete = true
				selectIssues(candidate, candidate.issues)
				return
			}
			val layout = candidate.layout!!
			if (candidate.probe == null && layout.rejections.isNotEmpty()) {
				selectIssues(
					candidate,
					Array(minOf(layout.rejections.size, MAX_GROUPS + 1)) { index ->
						val rejection = layout.rejections[index]
						issue("layout ${rejection.kind.name.lowercase()}: ${rejection.message}")
					},
				)
				return
			}
			val remaining = MAX_CELLS - visited
			val probe = candidate.probe
			val capturedLayout = if (probe == null) {
				if (layout.cells.size > remaining) {
					truncated = true
					PatternLayout(ObjectArrayList(layout.cells.subList(0, remaining)), layout.repeats, layout.choices)
				} else {
					layout
				}
			} else {
				if (PatternModuleSearch.scopedLayout(probe, remaining + 1).cells.size > remaining) truncated = true
				null
			}
			val input = if (probe != null) {
				PatternModuleSearch.prepare(pattern, state, controller.blockPos, candidate.front, up, candidate.flip, candidate.counts, probe, diagnostic = true, collection = collection, maximumCells = remaining)
			} else {
				PatternPreparation.prepare(pattern, pattern.getSourceDefinition(), capturedLayout!!, state, controller.blockPos, candidate.front, up, candidate.flip, false, candidate.counts, candidate.choices, bounds = layout, diagnostic = true, collection = collection)?.input
			}
			if (input == null) {
				incomplete = true
				if (candidate.probe != null) moduleOutcome = PatternModuleSearch.Outcome(false, true)
				selectIssues(candidate, safeExistingIssues())
				return
			}
			val diagnose = candidate.probe == null
			if (input.cellCount > remaining) truncated = true
			pendingInput = input
			// This lambda captures only detached worker input and an integer budget.
			pending = CompletableFuture.supplyAsync({ input.evaluate(diagnose, remaining) }, FormationExecutors.get((controller.level as ServerLevel).server).diagnosis)
		} catch (exception: RuntimeException) {
			GTCEu.LOGGER.error("Structure diagnosis failed for {} at {} ({})", controller, controller.blockPos, name, exception)
			finish(DiagnosisStatus.FAILED)
		}
	}

	fun progress(): DiagnosisReport = report ?: DiagnosisReport(DiagnosisStatus.RUNNING, name, visited, attempts, incomplete, truncated, null, ObjectLists.emptyList())

	private fun selectIssues(candidate: Candidate, issues: Array<PreparedPatternProblem>) {
		if (bestScore >= 0) return
		bestScore = 0
		basis = candidate
		bestGroups = Array(minOf(issues.size, MAX_GROUPS)) { index -> issues[index].let { PreparedProblemGroup(it, 1, longArrayOf(it.position)) } }
		if (issues.size > MAX_GROUPS) bestTruncated = true
	}

	private fun finish(status: DiagnosisStatus) {
		fun text(value: String): String {
			if (value.length > MAX_TEXT) truncated = true
			return value.take(MAX_TEXT)
		}
		fun choices(values: Object2ObjectMap<String, String>): Object2ObjectMap<String, String> {
			if (values.size <= MAX_METADATA) return values
			truncated = true
			return Object2ObjectMaps.emptyMap()
		}
		fun measured(values: Object2IntMap<String>): Object2IntMap<String> {
			if (values.size <= MAX_METADATA) return values
			truncated = true
			return Object2IntMaps.emptyMap()
		}
		val interpretation = basis?.let { candidate ->
			val modules = ObjectArrayList<DiagnosisModule>()
			val declared = candidate.layout?.moduleChoices
			if (declared != null) {
				for (index in 0 until minOf(declared.size, MAX_METADATA)) {
					val module = declared[index]
					modules.add(DiagnosisModule(text(module.descriptor.id), text(module.path), module.descriptor.kind.name, text(module.selection)))
				}
			}
			if ((candidate.layout?.moduleChoices?.size ?: 0) > MAX_METADATA || candidate.counts.size > MAX_METADATA) truncated = true
			DiagnosisBasis(
				candidate.front.name,
				candidate.flip,
				candidate.counts.copyOf(minOf(candidate.counts.size, MAX_METADATA)),
				choices(candidate.choices),
				measured(candidate.measurements),
				candidate.source,
				modules,
			)
		}
		val rows = ObjectArrayList<DiagnosisIssue>()
		var remaining = MAX_POSITIONS
		for ((first, occurrences, positions1) in bestGroups) {
			val limit = minOf(MAX_ROW_POSITIONS, remaining, positions1.size)
			val positions = positions1.copyOf(limit)
			remaining -= limit
			if (limit < positions1.size) truncated = true
			val position = BlockPos.of(first.position)
			val actual = if (state.world.isLoaded(position)) state.world.getBlockState(position).block.descriptionId else ""
			rows.add(
				DiagnosisIssue(
					text(first.expected),
					text(first.missingFact),
					first.difference,
					text(first.path),
					text(first.repeatPath),
					occurrences,
					positions,
					text(actual),
				),
			)
		}
		val structure = text(name)
		report = DiagnosisReport(status, structure, visited, attempts, incomplete, truncated || bestTruncated, interpretation, rows)
		close()
	}

	private fun candidates(exact: IntArray?, selected: Object2ObjectMap<String, String>, flip: Boolean?, manualMeasurements: Object2IntMap<String>): Sequence<Candidate> = sequence {
		val confirmed = controller.getMultiblockState(name).matchCache
		if (exact == null && selected.isEmpty() && flip == null && manualMeasurements.isEmpty() && controller.isStructureFormed(name) && confirmed != null) {
			val measurements = controller.getMultiblockState(name).facts.getOrDefault("pattern.measurements", Object2IntMaps.emptyMap<String>())
			yield(Candidate(confirmed.repetitions, PatternSelections.of(confirmed.selections), confirmed.front, confirmed.flipped, confirmed.layout, measurements, "confirmed"))
			return@sequence
		}
		val descriptors = pattern.getRepeatDescriptors()
		val counts = exact ?: IntArray(descriptors.size) { descriptors[it].minimum }
		suspend fun SequenceScope<Candidate>.explore(facing: Direction, mirrored: Boolean, choices: Object2ObjectMap<String, String>, measured: Object2IntMap<String>) {
			val layout = pattern.expandResolvedLayout(counts, choices, measured)
			val module = layout.moduleChoices.firstOrNull { !choices.containsKey(it.path) }
			if (module != null) {
				val search = PatternModuleSearch(pattern, counts, choices, measured, module.path)
				var continued = false
				for (step in search.steps) {
					when (step) {
						is PatternModuleSearch.Step.Probe -> {
							yield(Candidate(counts.copyOf(), step.selections, facing, mirrored, step.layout, measured, "module", probe = step))
							search.respond(moduleOutcome)
						}

						is PatternModuleSearch.Step.Continue -> {
							continued = true
							explore(facing, mirrored, step.selections, measured)
							search.respond(PatternModuleSearch.Outcome(lastCandidateMatched))
						}
					}
				}
				lastCandidateMatched = search.matched
				if (!continued && search.failurePath != null) yield(Candidate(counts.copyOf(), choices, facing, mirrored, layout, measured, "module", arrayOf(issue("unique choice branch at ${search.failurePath}"))))
				if (!continued && !search.unsupported && moduleProblems.isNotEmpty()) yield(Candidate(counts.copyOf(), choices, facing, mirrored, layout, measured, "module", moduleProblems))
				return
			}
			if (layout.rejections.isNotEmpty()) {
				yield(Candidate(counts.copyOf(), choices, facing, mirrored, layout, measured, "search"))
				return
			}
			val repeat = if (exact == null) layout.repeats.firstOrNull { it.descriptor.parameter.isEmpty() && !choices.containsKey("${it.path}#repeat") } else null
			if (repeat != null) {
				for (count in repeat.descriptor.minimum..repeat.descriptor.maximum) explore(facing, mirrored, PatternSelections.of(choices).with("${repeat.path}#repeat", count.toString()), measured)
				return
			}
			val choice = layout.choices.firstOrNull { !choices.containsKey(it.path) && !choices.containsKey(it.descriptor.id) }
			if (choice != null) {
				for (branch in if (choice.descriptor.priority.isEmpty()) choice.descriptor.alternatives else choice.descriptor.priority) explore(facing, mirrored, PatternSelections.of(choices).with(choice.path, branch), measured)
				return
			}
			yield(Candidate(counts.copyOf(), choices, facing, mirrored, layout, measured, if (exact != null || selected.isNotEmpty() || flip != null || manualMeasurements.isNotEmpty()) "manual" else "search"))
		}
		suspend fun SequenceScope<Candidate>.dimensions(index: Int, facing: Direction, mirrored: Boolean, measured: Object2IntMap<String>) {
			if (index == descriptors.size) {
				explore(facing, mirrored, selected, measured)
				return
			}
			val descriptor = descriptors[index]
			if (exact != null || descriptor.parameter.isEmpty() || measured.containsKey(descriptor.parameter)) {
				dimensions(index + 1, facing, mirrored, measured)
			} else {
				for (count in descriptor.minimum..descriptor.maximum) {
					counts[index] = count
					dimensions(index + 1, facing, mirrored, measured)
				}
			}
		}
		for (facing in if (controller.hasFrontFacing()) arrayOf(front) else arrayOf(Direction.SOUTH, Direction.NORTH, Direction.EAST, Direction.WEST)) {
			for (mirrored in if (flip != null) {
				arrayOf(flip)
			} else if (controller.allowFlip()) {
				arrayOf(false, true)
			} else {
				arrayOf(false)
			}) {
				if (!measurementsTrusted) {
					yield(Candidate(counts.copyOf(), selected, facing, mirrored, null, manualMeasurements, "measurement", safeExistingIssues(), true))
					continue
				}
				val measured = if (manualMeasurements.size == pattern.getMeasurements().size && manualMeasurements.isNotEmpty()) {
					manualMeasurements
				} else {
					val read = PatternMeasurements.read(pattern, state, controller.blockPos, facing, up, mirrored) {
						if (visited >= MAX_CELLS) {
							truncated = true
							false
						} else {
							visited++
							true
						}
					}
					if (read == null) {
						yield(Candidate(counts.copyOf(), selected, facing, mirrored, null, Object2IntLinkedOpenHashMap(), "measurement", measuredIssues()))
						continue
					}
					for (measurement in pattern.getMeasurements()) {
						val parameter = measurement.parameter
						if (manualMeasurements.containsKey(parameter)) read.put(parameter, manualMeasurements.getInt(parameter))
					}
					read
				}
				dimensions(0, facing, mirrored, measured)
				if (visited >= MAX_CELLS) return@sequence
			}
		}
	}

	private fun measuredIssues(): Array<PreparedPatternProblem> = state.matchResult?.diagnostics?.let(::issues) ?: arrayOf(issue("measurement boundary"))

	private fun issues(diagnostics: ObjectList<PatternDiagnostic>): Array<PreparedPatternProblem> = Array(minOf(diagnostics.size, MAX_GROUPS)) { index ->
		val (path, repeatPath, local, world, _, expected, missingFact, difference) = diagnostics[index]
		val (depth, height, width) = local
		PreparedPatternProblem(-1, path, repeatPath, depth, height, width, world.asLong(), expected, missingFact, difference)
	}

	/** Copies existing diagnostic values only; unknown error implementations and candidate suppliers are not invoked. */
	private fun safeExistingIssues(): Array<PreparedPatternProblem> {
		val existing = controller.getMultiblockState(name)
		val diagnostics = existing.matchResult?.diagnostics
		if (!diagnostics.isNullOrEmpty()) {
			return issues(diagnostics)
		}
		val error = existing.error
		val expected = when {
			error?.javaClass == PatternStringError::class.java -> (error as PatternStringError).translateKey
			error?.javaClass == SinglePredicateError::class.java -> "gtpm.multiblock.pattern.error.limited.${(error as SinglePredicateError).type}"
			else -> "gtpm.multiblock.diagnosis.incomplete"
		}
		return arrayOf(issue(expected))
	}

	private fun issue(expected: String): PreparedPatternProblem = PreparedPatternProblem(-1, "$", "", 0, 0, 0, controller.blockPos.asLong(), expected)

	override fun close() {
		if (closed) return
		closed = true
		if (report == null) report = DiagnosisReport(DiagnosisStatus.CANCELLED, name.take(MAX_TEXT), visited, attempts, incomplete, truncated, null, ObjectLists.emptyList())
		pendingInput?.cancel()
		pending?.cancel(false)
		pendingInput = null
		pending = null
		state.closeReadTracking()
	}

	companion object {
		const val MAX_CELLS = 2_000_000
		const val MAX_GROUPS = 64
		const val MAX_POSITIONS = 2048
		const val MAX_ROW_POSITIONS = 256
		private const val MAX_CANDIDATES = 4096
		private const val MAX_METADATA = 4096
		private const val MAX_TEXT = 4096
	}
}
