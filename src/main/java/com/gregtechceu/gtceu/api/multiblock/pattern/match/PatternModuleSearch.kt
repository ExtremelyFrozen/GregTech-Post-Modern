package com.gregtechceu.gtceu.api.multiblock.pattern.match

import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.api.multiblock.error.PatternStringError
import com.gregtechceu.gtceu.api.multiblock.pattern.layout.PatternAxis
import com.gregtechceu.gtceu.api.multiblock.pattern.layout.PatternLayout
import com.gregtechceu.gtceu.api.multiblock.pattern.layout.PatternLayoutCell
import com.gregtechceu.gtceu.api.multiblock.pattern.layout.PatternModuleDescriptor
import com.gregtechceu.gtceu.api.multiblock.pattern.layout.PatternModuleInstance
import com.gregtechceu.gtceu.api.multiblock.pattern.preparation.PatternPreparation
import com.gregtechceu.gtceu.api.multiblock.pattern.preparation.PatternWorldCollection
import com.gregtechceu.gtceu.api.multiblock.pattern.preparation.PreparedPatternInput

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction

import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.objects.Object2IntMap
import it.unimi.dsi.fastutil.objects.Object2ObjectMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList

/** Lazy module decisions shared by automatic matching and menu diagnosis. World reads belong to its driver. */
internal class PatternModuleSearch(private val pattern: MultiBlockPattern, private val repetitions: IntArray, selections: Map<String, String>, private val parameters: Object2IntMap<String>, private val initialPath: String) {
	sealed interface Step {
		data class Probe(val layout: PatternLayout, val prefix: String, val selections: Object2ObjectMap<String, String>) : Step
		data class Continue(val selections: Object2ObjectMap<String, String>) : Step
	}

	data class Outcome(val matched: Boolean, val unsupported: Boolean = false)
	private var selected = PatternSelections.of(selections)
	private var answer: Outcome? = null
	var matched: Boolean = false
		private set
	var unsupported: Boolean = false
		private set
	var failurePath: String? = null
		private set

	fun respond(outcome: Outcome) {
		answer = outcome
	}

	val steps: Sequence<Step> = sequence {
		val instance = layout().moduleChoices.first { it.path == initialPath }
		matched = module(instance) {
			request(Step.Continue(mark())).matched
		}
	}

	private fun mark(): PatternSelections = selected
	private fun restore(values: PatternSelections) {
		selected = values
	}
	private fun layout(): PatternLayout = pattern.expandResolvedLayout(repetitions, selected, parameters)
	private fun inside(path: String, prefix: String): Boolean = path == prefix || path.startsWith("$prefix/")

	private suspend fun SequenceScope<Step>.request(step: Step): Outcome {
		yield(step)
		val result = answer!!
		answer = null
		if (result.unsupported) unsupported = true
		return result
	}

	private suspend fun SequenceScope<Step>.probe(prefix: String): Boolean = !unsupported && request(Step.Probe(layout(), prefix, mark())).matched

	private suspend fun SequenceScope<Step>.scope(prefix: String, continuation: suspend SequenceScope<Step>.() -> Boolean): Boolean {
		if (unsupported) return false
		val layout = layout()
		val module = layout.moduleChoices.firstOrNull { inside(it.path, prefix) && !selected.containsKey(it.path) }
		if (module != null) return module(module) { scope(prefix, continuation) }
		val repeat = layout.repeats.firstOrNull { inside(it.path, prefix) && it.descriptor.parameter.isEmpty() && !selected.containsKey("${it.path}#repeat") }
		if (repeat != null) {
			val baseline = mark()
			for (count in repeat.descriptor.minimum..repeat.descriptor.maximum) {
				restore(baseline)
				selected = selected.with("${repeat.path}#repeat", count.toString())
				if (scope(prefix, continuation)) return true
				if (unsupported) break
			}
			restore(baseline)
			return false
		}
		val choice = layout.choices.firstOrNull { inside(it.path, prefix) && !selected.containsKey(it.path) && !selected.containsKey(it.descriptor.id) }
		if (choice != null) {
			val baseline = mark()
			var solution: PatternSelections? = null
			for (branch in if (choice.descriptor.priority.isEmpty()) choice.descriptor.alternatives else choice.descriptor.priority) {
				restore(baseline)
				selected = selected.with(choice.path, branch)
				if (scope(prefix, continuation)) {
					if (choice.descriptor.priority.isNotEmpty()) return true
					if (solution != null) {
						failurePath = choice.path
						restore(baseline)
						return false
					}
					solution = mark()
				}
				if (unsupported) break
			}
			restore(solution ?: baseline)
			return solution != null
		}
		return continuation()
	}

	private suspend fun SequenceScope<Step>.module(instance: PatternModuleInstance, continuation: suspend SequenceScope<Step>.() -> Boolean): Boolean {
		val baseline = mark()
		val descriptor = instance.descriptor
		if (descriptor.kind == PatternModuleDescriptor.Kind.CHAIN) {
			selected = selected.with(instance.path, descriptor.maximum.toString())
			val result = chain(instance, 0, continuation)
			if (!result) restore(baseline)
			return result
		}
		for (candidate in descriptor.candidates) {
			restore(baseline)
			selected = selected.with(instance.path, candidate)
			if (candidate == "absent" && descriptor.kind == PatternModuleDescriptor.Kind.OPTIONAL) return continuation()
			var locallyAccepted = false
			val prefix = "${instance.path}/segment[0]"
			val result = scope(prefix) {
				if (!probe(prefix)) {
					false
				} else {
					locallyAccepted = true
					continuation()
				}
			}
			if (result) return true
			// Later siblings never change a locally accepted optional or ordered alternative.
			if (locallyAccepted || unsupported) {
				restore(baseline)
				return false
			}
		}
		restore(baseline)
		return false
	}

	private suspend fun SequenceScope<Step>.chain(instance: PatternModuleInstance, index: Int, continuation: suspend SequenceScope<Step>.() -> Boolean): Boolean {
		val descriptor = instance.descriptor
		if (index == descriptor.maximum) return continuation()
		val baseline = mark()
		var segmentAccepted = false
		val prefix = "${instance.path}/segment[$index]"
		val result = scope(prefix) {
			if (!probe(prefix)) {
				false
			} else {
				segmentAccepted = true
				chain(instance, index + 1, continuation)
			}
		}
		if (result) return true
		if (unsupported || (segmentAccepted && !descriptor.backtrack) || index < descriptor.minimum) {
			restore(baseline)
			return false
		}
		// The first absent segment fixes the greedy end. Only backtracking chains can later shorten it.
		restore(baseline)
		selected = selected.with(instance.path, index.toString())
		return continuation()
	}

	companion object {
		/** A view copied in original layout order; no unrelated module or whole-layout rejection is probed. */
		private fun scopedLayout(step: Step.Probe): PatternLayout = PatternLayout(
			ObjectArrayList<PatternLayoutCell>().also { out ->
				for (cell in step.layout.cells) {
					if (cell.path == step.prefix || cell.path.startsWith("${step.prefix}/")) out.add(cell)
				}
			},
		)

		private fun prepare(pattern: MultiBlockPattern, target: MultiblockState, center: BlockPos, front: Direction, up: Direction, flipped: Boolean, repetitions: IntArray, step: Step.Probe, collection: PatternWorldCollection? = null): PreparedPatternInput? {
			val scoped = scopedLayout(step)
			val preparation = PatternPreparation.prepare(pattern, pattern.getSourceDefinition(), scoped, target, center, front, up, flipped, false, repetitions, step.selections, bounds = step.layout, collection = collection) ?: return null
			return preparation.input.forModuleProbe()
		}

		suspend fun evaluate(work: PatternWork<*>, pattern: MultiBlockPattern, target: MultiblockState, center: BlockPos, front: Direction, up: Direction, flipped: Boolean, repetitions: IntArray, step: Step.Probe, collection: PatternWorldCollection? = null): Boolean {
			val prepared = prepare(pattern, target, center, front, up, flipped, repetitions, step, collection = collection)
			if (prepared != null) {
				val result = work.compute(prepared)
				if (!result.matched) {
					val diagnostics = ObjectArrayList<PatternDiagnostic>()
					for ((_, path, repeatPath, depth, height, width, position, expected, missingFact, difference) in result.problems) {
						diagnostics.add(
							PatternDiagnostic(
								path,
								repeatPath,
								PatternCoordinate(depth, height, width),
								BlockPos.of(position),
								null,
								expected,
								missingFact,
								difference,
							),
						)
					}
					target.matchResult = PatternMatchResult(false, target.facts, diagnostics)
					target.setError(PatternStringError("gtpm.multiblock.pattern.error.constraint"))
				}
				return result.matched
			}
			return fallback(work, pattern, target, center, front, up, flipped, step, collection)
		}

		private suspend fun fallback(work: PatternWork<*>, pattern: MultiBlockPattern, target: MultiblockState, center: BlockPos, front: Direction, up: Direction, flipped: Boolean, step: Step.Probe, collection: PatternWorldCollection?): Boolean {
			val scratch = MultiblockState(target.world, target.controllerPos, target.structureName)
			scratch.lastController = target.lastController
			scratch.inheritReadTracking(target)
			scratch.clean()
			scratch.isNeededFlip = flipped
			for (axis in PatternAxis.entries) scratch.facts.set("pattern.${axis.name.lowercase()}", step.layout.size(axis))
			var previousDepth = 0
			fun failed(cell: PatternLayoutCell, position: BlockPos, expected: String): Boolean {
				val diagnostic = PatternDiagnostic(cell.path, cell.repeatPath, PatternCoordinate(cell.depth, cell.height, cell.width), position, null, expected, "", 0)
				target.matchResult = PatternMatchResult(false, target.facts, ObjectArrayList<PatternDiagnostic>().also { it.add(diagnostic) })
				target.setError(PatternStringError("gtpm.multiblock.pattern.error.constraint"))
				return false
			}
			for (cell in scopedLayout(step).cells) {
				work.checkpoint()
				if (previousDepth != cell.depth) {
					scratch.layerCount.clear()
					scratch.structureLayerCount.clear()
					previousDepth = cell.depth
				}
				val position = center.offset(pattern.getActualRelativeOffset(cell.width, cell.height, cell.depth, front, up, flipped))
				target.recordRead(position)
				target.addPosCache(position)
				val loaded = if (collection == null) {
					scratch.update(position, cell.predicate)
				} else {
					val captured = collection.read(target, position)
					if (captured.block == null) false else scratch.updateCached(position, cell.predicate, captured.block, captured.entity)
				}
				if (!loaded) return failed(cell, position, "loaded position")
				scratch.io = IO.BOTH
				val observation = cell.predicate.observePosition(scratch)
				val accepted = cell.predicate.acceptPosition(scratch, observation)
				val matched = accepted && pattern.matchesDirectionalPredicate(cell.predicate, scratch, front, up, flipped) && pattern.checkPartSharing(scratch.facts, scratch, cell.predicate)
				val dependencies = LongOpenHashSet()
				cell.predicate.collectDependencies(scratch, dependencies)
				val dependencyIterator = dependencies.iterator()
				while (dependencyIterator.hasNext()) {
					val dependency = BlockPos.of(dependencyIterator.nextLong())
					target.recordRead(dependency)
					target.addPosCache(dependency)
				}
				if (!matched) return failed(cell, position, cell.token)
				for ((key, value) in observation.properties) {
					val prior: Any? = scratch.facts.get(key)
					if (prior != null && prior != value) return failed(cell, position, "consistent $key")
					scratch.facts.set(key, value)
				}
				val positionFacts = ObjectArrayList<String>()
				positionFacts.add("token:${cell.token}")
				positionFacts.addAll(observation.facts)
				for (alias in cell.predicate.facts) if (!positionFacts.contains(alias)) positionFacts.add(alias)
				scratch.facts.ensureScopes(cell.scopes)
				for (fact in positionFacts) {
					if (!cell.predicate.facts.contains(fact)) scratch.facts.addFact(fact)
					scratch.facts.addScopedFact(fact, cell.scopes)
				}
			}
			return true
		}
	}
}
