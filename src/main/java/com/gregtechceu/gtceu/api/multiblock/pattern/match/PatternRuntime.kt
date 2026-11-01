package com.gregtechceu.gtceu.api.multiblock.pattern.match

import com.gregtechceu.gtceu.api.block.ActiveBlock
import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.api.multiblock.error.PatternStringError
import com.gregtechceu.gtceu.api.multiblock.pattern.constraint.PatternConstraintEvaluator
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternConstraint
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternDefinition

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.objects.*

/** Evaluates layouts transactionally per attempt; the controller remains responsible for lifecycle changes. */
internal class PatternRuntime(private val pattern: MultiBlockPattern, private val definition: PatternDefinition) {
	var lastResult: PatternMatchResult? = null
		private set
	var lastRepetitions: IntArray = IntArray(0)
		private set

	fun match(state: MultiblockState, center: BlockPos, front: Direction, up: Direction, flipped: Boolean, save: Boolean, repetitions: IntArray, choices: Map<String, String>): PatternMatchResult {
		state.clean()
		state.isNeededFlip = flipped
		val errors = ObjectArrayList<PatternDiagnostic>()
		val facts = state.facts
		val branchChoices = Object2ObjectOpenHashMap<String, String>()
		val instanceCounts = Object2IntOpenHashMap<String>()
		choices.forEach { (key, value) -> if (key.endsWith("#repeat")) instanceCounts[key.removeSuffix("#repeat")] = value.toInt() else branchChoices[key] = value }
		facts.set("pattern.choices", branchChoices)
		facts.set("pattern.instance_repetitions", instanceCounts)
		facts.set("pattern.repetitions", repetitions.copyOf())
		fun error(path: String, repeatPath: String, depth: Int, height: Int, width: Int, pos: BlockPos, expected: String, fact: String = "", difference: Int = 0) {
			errors.add(PatternDiagnostic(path, repeatPath, PatternCoordinate(depth, height, width), pos, if (state.world.isLoaded(pos)) state.world.getBlockState(pos) else null, expected, fact, difference))
		}
		if ((flipped && !definition.orientation.allowMirror) || (front.axis == Direction.Axis.Y && !definition.orientation.allowExtendedFacing)) {
			error("$", "", 0, 0, 0, center, "orientation")
		}
		var previousDepth: Int? = null
		val layout = pattern.expandLayoutForMatch(repetitions, choices)
		layout.repeats.forEach { facts.ensureScopes(ObjectLists.singleton("repeat:${it.descriptor.id}@${it.path}")) }
		val cells = ObjectArrayList(layout.cells)
		cells.sortWith(compareBy { it.depth })
		for ((path, repeatPath, depth, height, width, predicate, token, scopes) in cells) {
			if (previousDepth != depth) {
				if (previousDepth != null && !pattern.checkLayerMinimums(state, state.layerCount, state.structureLayerCount)) {
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
			val position = center.offset(pattern.getActualRelativeOffset(width, height, depth, front, up, flipped))
			if (!state.update(position, predicate)) {
				error(path, repeatPath, depth, height, width, position, "loaded position")
				break
			}
			state.io = IO.BOTH
			val observation = predicate.observePosition(state)
			val accepted = predicate.acceptPosition(state, observation)
			if (!accepted || !pattern.matchesDirectionalPredicate(predicate, state, front, up, flipped) || !pattern.checkPartSharing(
					facts,
					state,
					predicate,
				)
			) {
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
		}
		if (errors.isEmpty && (!pattern.checkLayerMinimums(state, state.layerCount, state.structureLayerCount) || !pattern.checkGlobalMinimums(state, state.globalCount, state.structureGlobalCount))) error("$", "", 0, 0, 0, center, "minimum count", "", 1)
		definition.constraints.forEach { constraint ->
			if (constraint is PatternConstraint.Count) {
				PatternConstraintEvaluator.entries(constraint, facts).object2IntEntrySet().forEach { entry ->
					val count = entry.intValue
					if (count !in constraint.minimum..constraint.maximum) {
						val cell = cells.firstOrNull { it.scopes.contains(entry.key) }
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
		val result = PatternMatchResult(errors.isEmpty, facts.copy(), ObjectLists.unmodifiable(errors))
		lastResult = result
		state.facts.set("pattern.diagnostics", result.diagnostics)
		state.setError(if (result.matched) null else PatternStringError("gtpm.multiblock.pattern.error.constraint"))
		state.isNeededFlip = flipped
		return result
	}

	fun search(state: MultiblockState, center: BlockPos, front: Direction, up: Direction, flipped: Boolean, save: Boolean): Boolean {
		val descriptors = pattern.getRepeatDescriptors()
		val repetitions = IntArray(descriptors.size) { descriptors[it].minimum }
		val selected = Object2ObjectOpenHashMap<String, String>()
		fun explore(): Boolean {
			val layout = pattern.expandLayoutForMatch(repetitions, selected)
			val repeat = layout.repeats.firstOrNull { !selected.containsKey("${it.path}#repeat") }
			if (repeat != null) {
				val baseline = Object2ObjectOpenHashMap(selected)
				for (count in repeat.descriptor.minimum..repeat.descriptor.maximum) {
					selected.clear()
					selected.putAll(baseline)
					selected["${repeat.path}#repeat"] = count.toString()
					if (explore()) return true
				}
				selected.clear()
				selected.putAll(baseline)
				return false
			}
			val choice = layout.choices.firstOrNull { !selected.containsKey(it.path) }
			if (choice == null) return match(state, center, front, up, flipped, save, repetitions, selected).matched
			val branch = choice.descriptor
			val baseline = Object2ObjectOpenHashMap(selected)
			var solution: Object2ObjectOpenHashMap<String, String>? = null
			for (alternative in if (branch.priority.isEmpty()) branch.alternatives else branch.priority) {
				selected.clear()
				selected.putAll(baseline)
				selected[choice.path] = alternative
				if (explore()) {
					if (branch.priority.isNotEmpty()) return true
					if (solution != null) {
						val error = PatternDiagnostic(choice.path, "", PatternCoordinate(0, 0, 0), center, null, "unique choice branch", "", 1)
						lastResult = PatternMatchResult(false, state.facts.copy(), ObjectLists.singleton(error))
						state.setError(PatternStringError("gtpm.multiblock.pattern.error.ambiguous_choice"))
						return false
					}
					solution = Object2ObjectOpenHashMap(selected)
				}
			}
			selected.clear()
			selected.putAll(solution ?: baseline)
			return solution != null && match(state, center, front, up, flipped, save, repetitions, selected).matched
		}
		val matched = explore()
		if (matched) {
			val resolved = BooleanArray(descriptors.size)
			pattern.expandLayoutForMatch(repetitions, selected).repeats.forEach { instance ->
				val index = instance.descriptor.index
				if (!resolved[index]) {
					repetitions[index] = selected.getValue("${instance.path}#repeat").toInt()
					resolved[index] = true
				}
			}
			lastRepetitions = repetitions.copyOf()
			return match(state, center, front, up, flipped, save, repetitions, selected).matched
		}
		return matched
	}
}
