package com.gregtechceu.gtceu.api.multiblock.pattern.match

import com.gregtechceu.gtceu.api.block.ActiveBlock
import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.api.multiblock.error.PatternStringError
import com.gregtechceu.gtceu.api.multiblock.pattern.constraint.PatternConstraintEvaluator
import com.gregtechceu.gtceu.api.multiblock.pattern.layout.PatternAxis
import com.gregtechceu.gtceu.api.multiblock.pattern.layout.PatternLayoutCell
import com.gregtechceu.gtceu.api.multiblock.pattern.layout.PatternRepeatDescriptor
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternConstraint
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternDefinition
import com.gregtechceu.gtceu.api.multiblock.pattern.preparation.PatternPreparation
import com.gregtechceu.gtceu.api.multiblock.pattern.preparation.PatternWorldCollection

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.longs.LongSet
import it.unimi.dsi.fastutil.longs.LongSets
import it.unimi.dsi.fastutil.objects.*

/** Evaluates layouts transactionally per attempt; the controller remains responsible for lifecycle changes. */
internal class PatternRuntime(private val pattern: MultiBlockPattern, private val definition: PatternDefinition, private val previous: PatternMatchCache? = null, private val changed: LongSet = LongSets.emptySet()) {
	private var collection = PatternWorldCollection()
	private var measured: Object2IntMap<String> = Object2IntMaps.emptyMap()
	var lastResult: PatternMatchResult? = null
		private set
	var lastRepetitions: IntArray = IntArray(0)
		private set

	fun match(state: MultiblockState, center: BlockPos, front: Direction, up: Direction, flipped: Boolean, save: Boolean, repetitions: IntArray, choices: Map<String, String>): PatternMatchResult {
		collection = PatternWorldCollection()
		state.beginReadTracking()
		try {
			return PatternWork { matchExact(this, state, center, front, up, flipped, save, repetitions, choices) }.finish()
		} finally {
			state.closeReadTracking()
		}
	}

	internal suspend fun matchExact(work: PatternWork<*>, state: MultiblockState, center: BlockPos, front: Direction, up: Direction, flipped: Boolean, save: Boolean, repetitions: IntArray, choices: Map<String, String>): PatternMatchResult {
		val selections = PatternSelections.of(choices)
		state.clean()
		measured = PatternMeasurements.read(pattern, state, center, front, up, flipped) ?: return state.matchResult
		for (measurement in pattern.getMeasurements()) {
			val requested = choices["@measure:${measurement.parameter}"] ?: continue
			if (requested.toInt() != measured.getValue(measurement.parameter)) {
				val problem = PatternDiagnostic("$/measurement:${measurement.id}", "", PatternCoordinate(0, 0, 0), center, null, "measured ${measurement.parameter}", measurement.parameter, requested.toInt() - measured.getValue(measurement.parameter))
				state.setError(PatternStringError("gtpm.multiblock.pattern.error.constraint"))
				return PatternMatchResult(false, state.facts, ObjectLists.singleton(problem)).also { state.matchResult = it }
			}
		}
		return match(work, state, center, front, up, flipped, save, repetitions, selections)
	}

	internal suspend fun match(work: PatternWork<*>, target: MultiblockState, center: BlockPos, front: Direction, up: Direction, flipped: Boolean, save: Boolean, repetitions: IntArray, choices: Object2ObjectMap<String, String>): PatternMatchResult {
		val state = MultiblockState(target.world, target.controllerPos, target.structureName)
		state.lastController = target.lastController
		state.inheritReadTracking(target)
		state.clean()
		state.isNeededFlip = flipped
		val errors = ObjectArrayList<PatternDiagnostic>()
		val facts = state.facts
		val instanceCounts = Object2IntOpenHashMap<String>()
		choices.forEach { (key, value) -> if (key.endsWith("#repeat")) instanceCounts[key.removeSuffix("#repeat")] = value.toInt() }
		facts.set("pattern.choices", choices)
		facts.set("pattern.instance_repetitions", instanceCounts)
		facts.set("pattern.repetitions", repetitions.copyOf())
		fun error(path: String, repeatPath: String, depth: Int, height: Int, width: Int, pos: BlockPos, expected: String, fact: String = "", difference: Int = 0) {
			errors.add(PatternDiagnostic(path, repeatPath, PatternCoordinate(depth, height, width), pos, if (state.world.isLoaded(pos)) state.world.getBlockState(pos) else null, expected, fact, difference))
		}
		if ((flipped && !definition.orientation.allowMirror) || (front.axis == Direction.Axis.Y && !definition.orientation.allowExtendedFacing)) {
			error("$", "", 0, 0, 0, center, "orientation")
		}
		var previousDepth = 0
		val layout = pattern.expandLayoutForMatch(work, repetitions, choices, measured)
		facts.set("pattern.measurements", measured)
		layout.rejections.forEach { rejection -> error("$", "", 0, 0, 0, center, rejection.message) }
		if (errors.isNotEmpty()) {
			val result = PatternMatchResult(false, facts, ObjectLists.unmodifiable(errors))
			lastResult = result
			state.matchResult = result
			state.setError(PatternStringError("gtpm.multiblock.pattern.error.constraint"))
			target.applyMatch(state)
			return result
		}
		val preparation = PatternPreparation.prepare(pattern, definition, layout, state, center, front, up, flipped, save, repetitions, choices, previous?.preparation, changed, collection = collection)
		if (preparation != null) {
			val evaluated = work.compute(preparation.input)
			val prepared = preparation.apply(evaluated)
			prepared.facts.set("pattern.measurements", measured)
			prepared.matchResult.facts.set("pattern.measurements", measured)
			lastResult = prepared.matchResult
			target.applyMatch(prepared)
			return prepared.matchResult
		}
		facts.set("pattern.width", layout.size(PatternAxis.WIDTH))
		facts.set("pattern.height", layout.size(PatternAxis.HEIGHT))
		facts.set("pattern.depth", layout.size(PatternAxis.DEPTH))
		for ((path, descriptor) in layout.repeats) {
			work.checkpoint()
			facts.ensureScopes(ObjectLists.singleton("repeat:${descriptor.id}@$path"))
		}
		val cells = layout.cells
		val widthOffset = pattern.getActualRelativeOffset(1, 0, 0, front, up, flipped)
		val heightOffset = pattern.getActualRelativeOffset(0, 1, 0, front, up, flipped)
		val depthOffset = pattern.getActualRelativeOffset(0, 0, 1, front, up, flipped)
		val matchCache = PatternMatchCache(pattern, layout, center, front, up, flipped, repetitions.copyOf(), choices)
		val reusable = previous?.takeIf { changed.isNotEmpty() && it.layout === layout && it.center == center && it.front == front && it.up == up && it.flipped == flipped }
		var cellIndex = 0
		for ((path, repeatPath, depth, height, width, predicate, token, scopes) in cells) {
			work.checkpoint()
			if (cellIndex == 0 || previousDepth != depth) {
				if (cellIndex > 0 && !pattern.checkLayerMinimums(state, state.layerCount, state.structureLayerCount)) {
					error(
						path,
						repeatPath,
						depth,
						height,
						width,
						center,
						"layer minimum",
					)
					break
				}
				state.layerCount.clear()
				state.structureLayerCount.clear()
				previousDepth = depth
			}
			val position = BlockPos(
				center.x + width * widthOffset.x + height * heightOffset.x + depth * depthOffset.x,
				center.y + width * widthOffset.y + height * heightOffset.y + depth * depthOffset.y,
				center.z + width * widthOffset.z + height * heightOffset.z + depth * depthOffset.z,
			)
			state.addPosCache(position)
			val cached = reusable?.positions?.get(cellIndex)?.takeIf { predicate.hasPositionDependencies() && reusable.unchanged(it, changed) }
			cellIndex++
			if (!(if (cached == null) state.update(position, predicate) else state.updateCached(position, predicate, cached.block, cached.entity))) {
				state.addPosCache(position)
				error(path, repeatPath, depth, height, width, position, "loaded position")
				break
			}
			state.io = IO.BOTH
			val observation = when {
				cached == null -> predicate.observePosition(state)
				predicate.canReuseObservation() -> cached.observation
				else -> predicate.observePosition(state, cached.observation)
			}
			val accepted = predicate.acceptPosition(state, observation)
			if (!accepted || !pattern.matchesDirectionalPredicate(predicate, state, front, up, flipped) || !pattern.checkPartSharing(
					facts,
					state,
					predicate,
				)
			) {
				val failedDependencies = LongOpenHashSet()
				predicate.collectDependencies(state, failedDependencies)
				val dependencies = failedDependencies.iterator()
				while (dependencies.hasNext()) {
					val dependency = BlockPos.of(dependencies.nextLong())
					state.addPosCache(dependency)
					state.recordRead(dependency)
				}
				state.addPosCache(position)
				val failure = PatternDiagnostic(path, repeatPath, PatternCoordinate(depth, height, width), position, if (state.world.isLoaded(position)) state.world.getBlockState(position) else null, token, "", 0)
				errors.add(predicate.diagnose(failure))
				break
			}
			val positionFacts = ObjectOpenHashSet<String>()
			positionFacts.add("token:$token")
			positionFacts.addAll(observation.facts)
			observation.properties.forEach { (key, value) ->
				val prior: Any? = facts.get(key)
				if (prior != null && prior != value) {
					error(
						path,
						repeatPath,
						depth,
						height,
						width, position, "consistent $key", key, 1,
					)
				}
				facts.set(key, value)
			}
			positionFacts.addAll(predicate.facts)
			facts.ensureScopes(scopes)
			positionFacts.forEach { fact ->
				if (!predicate.facts.contains(fact)) facts.addFact(fact)
				facts.addScopedFact(
					fact,
					scopes,
				)
			}
			pattern.saveMatch(facts, state, predicate, position, save)
			if (state.blockState.block is ActiveBlock) facts.getOrCreate("vaBlocks") { LongOpenHashSet() }.add(position.asLong())
			facts.getOrCreate("ioMap") { Long2ObjectOpenHashMap<Any?>() }[position.asLong()] = state.io
			matchCache.add(state, cells[cellIndex - 1], observation)
		}
		if (errors.isEmpty && (!pattern.checkLayerMinimums(state, state.layerCount, state.structureLayerCount) || !pattern.checkGlobalMinimums(state, state.globalCount, state.structureGlobalCount))) error("$", "", 0, 0, 0, center, "minimum count", "", 1)
		for (constraint in definition.constraints) {
			work.checkpoint()
			if (constraint is PatternConstraint.Count) {
				for (entry in PatternConstraintEvaluator.entries(constraint, facts).object2IntEntrySet()) {
					work.checkpoint()
					val count = entry.intValue
					if (count !in constraint.minimum..constraint.maximum) {
						var cell: PatternLayoutCell? = null
						for (candidate in cells) {
							work.checkpoint()
							if (candidate.scopes.contains(entry.key)) {
								cell = candidate
								break
							}
						}
						val instance = layout.repeats.firstOrNull { entry.key == "repeat:${it.descriptor.id}@${it.path}" }
						val depth = cell?.depth ?: instance?.depth ?: 0
						val height = cell?.height ?: instance?.height ?: 0
						val width = cell?.width ?: instance?.width ?: 0
						val pos = center.offset(pattern.getActualRelativeOffset(width, height, depth, front, up, flipped))
						error(entry.key.substringAfter('@', "$/constraint:${entry.key}"), cell?.repeatPath ?: instance?.path ?: "", depth, height, width, pos, constraint.message, constraint.fact, if (count < constraint.minimum) constraint.minimum - count else constraint.maximum - count)
					}
				}
			}
		}
		val result = PatternMatchResult(errors.isEmpty, facts, ObjectLists.unmodifiable(errors))
		lastResult = result
		state.facts.set("pattern.diagnostics", result.diagnostics)
		state.setError(if (result.matched) null else PatternStringError("gtpm.multiblock.pattern.error.constraint"))
		state.isNeededFlip = flipped
		state.matchResult = result
		if (result.matched) state.matchCache = matchCache
		target.applyMatch(state)
		return result
	}

	fun search(state: MultiblockState, center: BlockPos, front: Direction, up: Direction, flipped: Boolean, save: Boolean): Boolean {
		collection = PatternWorldCollection()
		state.beginReadTracking()
		try {
			return PatternWork { search(this, state, center, front, up, flipped, save) }.finish()
		} finally {
			state.closeReadTracking()
		}
	}

	internal suspend fun search(work: PatternWork<*>, state: MultiblockState, center: BlockPos, front: Direction, up: Direction, flipped: Boolean, save: Boolean): Boolean {
		measured = PatternMeasurements.read(pattern, state, center, front, up, flipped) ?: return false
		val descriptors = pattern.getRepeatDescriptors()
		val repetitions = IntArray(descriptors.size) { descriptors[it].minimum }
		var selected = PatternSelections.empty()
		suspend fun explore(): Boolean {
			work.checkpoint()
			val layout = pattern.expandLayoutForMatch(work, repetitions, selected, measured)
			val module = layout.moduleChoices.firstOrNull { !selected.containsKey(it.path) }
			if (module != null) {
				val baseline = selected
				val search = PatternModuleSearch(pattern, repetitions, baseline, measured, module.path)
				var moduleSolution: MultiblockState? = null
				var moduleSelections: PatternSelections? = null
				for (step in search.steps) {
					work.checkpoint()
					val matched = when (step) {
						is PatternModuleSearch.Step.Probe -> PatternModuleSearch.evaluate(work, pattern, state, center, front, up, flipped, repetitions, step, collection)

						is PatternModuleSearch.Step.Continue -> {
							selected = PatternSelections.of(step.selections)
							explore().also {
								if (it) {
									moduleSolution = state.captureMatch()
									moduleSelections = selected
								}
							}
						}
					}
					search.respond(PatternModuleSearch.Outcome(matched))
				}
				if (search.matched) {
					selected = moduleSelections!!
					moduleSolution?.let(state::applyMatch)
					lastResult = state.matchResult
					return true
				}
				search.failurePath?.let { path ->
					val error = PatternDiagnostic(path, "", PatternCoordinate(0, 0, 0), center, null, "unique choice branch", "", 1)
					lastResult = PatternMatchResult(false, state.facts, ObjectLists.singleton(error))
					state.matchResult = lastResult
					state.setError(PatternStringError("gtpm.multiblock.pattern.error.ambiguous_choice"))
				}
				lastResult = state.matchResult
				selected = baseline
				return false
			}
			val repeat = layout.repeats.firstOrNull { it.descriptor.parameter.isEmpty() && !selected.containsKey("${it.path}#repeat") }
			if (repeat != null) {
				val baseline = selected
				for (count in repeat.descriptor.minimum..repeat.descriptor.maximum) {
					selected = baseline.with("${repeat.path}#repeat", count.toString())
					if (explore()) return true
				}
				selected = baseline
				return false
			}
			val choice = layout.choices.firstOrNull { !selected.containsKey(it.path) }
			if (choice == null) return match(work, state, center, front, up, flipped, save, repetitions, selected).matched
			val branch = choice.descriptor
			val baseline = selected
			var solution: PatternSelections? = null
			var solutionState: MultiblockState? = null
			for (alternative in if (branch.priority.isEmpty()) branch.alternatives else branch.priority) {
				selected = baseline.with(choice.path, alternative)
				if (explore()) {
					if (branch.priority.isNotEmpty()) return true
					if (solution != null) {
						val error = PatternDiagnostic(choice.path, "", PatternCoordinate(0, 0, 0), center, null, "unique choice branch", "", 1)
						lastResult = PatternMatchResult(false, state.facts, ObjectLists.singleton(error))
						state.setMatchResult(lastResult)
						state.setError(PatternStringError("gtpm.multiblock.pattern.error.ambiguous_choice"))
						return false
					}
					solution = selected
					solutionState = state.captureMatch()
				}
			}
			selected = solution ?: baseline
			if (solutionState == null) return false
			state.applyMatch(solutionState)
			lastResult = state.matchResult
			return true
		}
		val parameters = ObjectArrayList<PatternRepeatDescriptor>()
		for (descriptor in descriptors) if (descriptor.parameter.isNotEmpty()) parameters.add(descriptor)
		suspend fun dimensions(index: Int): Boolean {
			if (index == parameters.size) return explore()
			val parameter = parameters[index]
			for (count in parameter.minimum..parameter.maximum) {
				repetitions[parameter.index] = count
				if (dimensions(index + 1)) return true
			}
			return false
		}
		val matched = dimensions(0)
		if (matched) {
			val resolved = BooleanArray(descriptors.size)
			pattern.expandLayoutForMatch(work, repetitions, selected, measured).repeats.forEach { instance ->
				val index = instance.descriptor.index
				if (instance.descriptor.parameter.isEmpty() && !resolved[index]) {
					repetitions[index] = selected.getValue("${instance.path}#repeat").toInt()
					resolved[index] = true
				}
			}
			lastRepetitions = repetitions.copyOf()
			state.facts.set("pattern.repetitions", lastRepetitions)
			state.facts.set("pattern.measurements", measured)
			state.matchResult.facts.set("pattern.repetitions", lastRepetitions)
			state.matchCache?.repetitions?.let { lastRepetitions.copyInto(it) }
			lastResult = state.matchResult
			return true
		}
		return matched
	}
}
