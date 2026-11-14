package com.gregtechceu.gtceu.api.multiblock.pattern.preparation

import it.unimi.dsi.fastutil.booleans.BooleanArrayList
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet

/** Interprets detached rules in layout order. No game or predicate API is available in this file. */
internal class PreparedPatternEvaluator(private val input: PreparedPatternInput, private val indices: IntArray? = null, private val evaluateConstraints: Boolean = true) {
	private val global = IntArray(input.counters.size) { -1 }
	private val layer = IntArray(input.counters.size) { -1 }
	private val properties = Object2IntOpenHashMap<String>().also { it.defaultReturnValue(-1) }
	private val propertyReferences = Object2ObjectLinkedOpenHashMap<String, PreparedProperty>()
	private val counts = Object2IntOpenHashMap<String>()
	private val scopes = Object2ObjectLinkedOpenHashMap<String, Object2IntOpenHashMap<String>>()
	private val problems = ObjectArrayList<PreparedPatternProblem>()
	private val accepted = ObjectArrayList<PreparedAcceptedCell>()
	private val effects = ObjectArrayList<PreparedEffect>()
	private val positionFacts = ObjectLinkedOpenHashSet<String>()
	private var currentCell = -1
	private var io = 0

	fun evaluate(): PreparedPatternResult {
		input.checkpoint()
		input.initialScopes.forEach { scopes.computeIfAbsent(it.name) { Object2IntOpenHashMap() } }
		if (!input.orientationValid) problem(-1, "orientation", kind = PreparedProblemKind.ORIENTATION)
		var previousLayer = -1
		for (index in indices ?: IntArray(input.cells.size) { it }) {
			input.checkpoint()
			if (problems.isNotEmpty()) break
			currentCell = index
			val cell = input.cells[index]
			if (previousLayer != input.layerRuns[index]) {
				if (previousLayer >= 0 && !minimums(layer, true)) {
					break
				}
				layer.fill(-1)
				previousLayer = input.layerRuns[index]
			}
			if (!cell.loaded) {
				problem(index, "loaded position", kind = PreparedProblemKind.UNLOADED)
				continue
			}
			io = 0
			val observation = observe(cell.root)
			val acceptedPosition = accept(cell.root, observation)
			if (!acceptedPosition || !cell.directional || !cell.shareable) {
				problem(
					index,
					when {
						!acceptedPosition -> cell.token
						!cell.directional -> "gtpm.structure.issue.orientation"
						else -> "multiblocked.pattern.error.share"
					},
					kind = when {
						!acceptedPosition -> PreparedProblemKind.PREDICATE
						!cell.directional -> PreparedProblemKind.ORIENTATION
						else -> PreparedProblemKind.SHARED
					},
				)
				continue
			}
			for (property in observation.properties) {
				val previous = properties.put(property.key, property.value)
				propertyReferences[property.key] = property
				if (previous >= 0 && previous != property.value) {
					problem(index, "consistent ${property.key}", property.key, 1, kind = PreparedProblemKind.MIXED)
				}
			}
			val aliases = when (val root = cell.root) {
				is PreparedNode.Pattern -> root.aliases
				is PreparedNode.Observed -> root.aliases
				else -> emptyArray()
			}
			positionFacts.clear()
			positionFacts.add("token:${cell.token}")
			for (fact in observation.facts) positionFacts.add(fact)
			for (alias in aliases) positionFacts.add(alias)
			for (scope in cell.scopes) scopes.computeIfAbsent(scope) { Object2IntOpenHashMap() }
			for (fact in positionFacts) {
				if (!aliases.contains(fact)) counts.addTo(fact, 1)
				for (scope in cell.scopes) scopes.getValue(scope).addTo(fact, 1)
			}
			accepted.add(PreparedAcceptedCell(index, observation, io))
		}
		if (problems.isEmpty && minimums(layer, true)) minimums(global, false)
		for (requirement in input.requirements) {
			if (!evaluateConstraints) break
			if (problems.isNotEmpty()) break
			if (requirement.prefix == "all") {
				constraint(requirement, "all", counts.getInt(requirement.fact))
			} else {
				var found = false
				for ((scope, values) in scopes) {
					if (scope.startsWith("${requirement.prefix}@")) {
						found = true
						constraint(requirement, scope, values.getInt(requirement.fact))
						if (problems.isNotEmpty()) break
					}
				}
				if (!found) constraint(requirement, requirement.prefix, 0)
			}
		}
		return PreparedPatternResult(
			problems.isEmpty, problems.toTypedArray(), accepted.toTypedArray(), effects.toTypedArray(),
			PreparedContributionArrays.facts(counts), scopes.keys.toTypedArray(),
			PreparedContributionArrays.scoped(scopes),
			propertyReferences.values.toTypedArray(), global, layer, currentCell, io,
		)
	}

	private fun observe(node: PreparedNode): PreparedObservation {
		input.checkpoint()
		return when (node) {
			is PreparedNode.Leaf -> {
				val current = if (node.consistency.isEmpty()) -1 else properties.getInt(node.consistency)
				val value = node.properties.firstOrNull { it.key == node.consistency }?.value ?: -1
				if (node.matched && (current < 0 || current == value)) PreparedObservation(node.id, true, node.facts, node.properties) else failed(node)
			}

			is PreparedNode.Restricted -> if (!capacity(node.counter)) {
				failed(node)
			} else {
				val child = observe(node.child)
				child.copy(node = node.id, children = arrayOf(child), selected = IntArray(0), available = BooleanArray(0))
			}

			is PreparedNode.Alternative -> {
				val children = Array(node.children.size) { observe(node.children[it]) }
				val first = children.firstOrNull { it.matched }
				PreparedObservation(node.id, first != null, first?.facts ?: emptyArray(), first?.properties ?: emptyArray(), children)
			}

			is PreparedNode.DataHatch -> {
				val data = node.data?.let(::observe)
				if (data?.matched == true) {
					data.copy(node = node.id, children = arrayOf(data), selected = intArrayOf(0))
				} else {
					val alternative = observe(node.alternative)
					alternative.copy(node = node.id, children = arrayOf(alternative), selected = intArrayOf(1))
				}
			}

			is PreparedNode.Pattern -> observePattern(node)

			is PreparedNode.Combined -> {
				val children = Array(node.branches.size) { observe(node.branches[it]) }
				val success = IntArrayList(children.size)
				for (index in children.indices) if (children[index].matched) success.add(index)
				val matched = when (node.selection) {
					0 -> success.size == children.size
					1 -> success.isNotEmpty()
					2 -> success.size == 1
					else -> success.isEmpty
				}
				if (!matched) {
					failed(node)
				} else {
					val chosen = when (node.selection) {
						0 -> success.toIntArray()

						3 -> IntArray(0)

						else -> {
							var preferred = success.getInt(0)
							for (index in 1 until success.size) {
								val candidate = success.getInt(index)
								if (node.priorities[candidate] < node.priorities[preferred]) preferred = candidate
							}
							intArrayOf(preferred)
						}
					}
					val result = merge(node.id, children, chosen)
					result.copy(facts = aliases(result, node.aliases))
				}
			}

			is PreparedNode.Observed -> {
				var hasLimited = false
				var available = false
				for ((_, counter, limited) in node.rules) {
					if (limited) {
						hasLimited = true
						if (capacity(counter)) available = true
					}
				}
				if (if (hasLimited) !available else node.rules.isEmpty()) {
					failed(node)
				} else {
					val expression = observe(node.expression)
					expression.copy(node = node.id, facts = aliases(expression, node.aliases), children = arrayOf(expression), selected = IntArray(0), available = BooleanArray(0))
				}
			}
		}
	}

	private fun observePattern(node: PreparedNode.Pattern): PreparedObservation {
		val observations = ObjectArrayList<PreparedObservation>()
		val available = BooleanArrayList(node.rules.size)
		val selected = IntArrayList(node.rules.size)
		for ((source, counter, limited) in node.rules) {
			if (!limited && selected.isNotEmpty()) break
			val canAccept = !limited || capacity(counter)
			val observed = if (canAccept) observe(source) else failed(source)
			available.add(canAccept)
			observations.add(observed)
			if (observed.matched) selected.add(observations.lastIndex)
		}
		if (selected.isEmpty) return PreparedObservation(node.id, false, children = observations.toTypedArray(), available = available.toBooleanArray())
		val result = merge(node.id, observations.toTypedArray(), selected.toIntArray())
		return result.copy(facts = aliases(result, node.aliases), available = available.toBooleanArray())
	}

	private fun aliases(observation: PreparedObservation, aliases: Array<String>): Array<String> = if (!observation.matched) {
		emptyArray()
	} else {
		ObjectLinkedOpenHashSet<String>(observation.facts.size + aliases.size).also { facts ->
			for (fact in observation.facts) facts.add(fact)
			for (alias in aliases) facts.add(alias)
		}.toTypedArray()
	}

	private fun merge(id: Int, children: Array<PreparedObservation>, selected: IntArray): PreparedObservation {
		val facts = ObjectLinkedOpenHashSet<String>()
		val values = Object2IntOpenHashMap<String>().also { it.defaultReturnValue(-1) }
		val references = Object2ObjectLinkedOpenHashMap<String, PreparedProperty>()
		for (index in selected) {
			for (fact in children[index].facts) facts.add(fact)
			for (property in children[index].properties) {
				val previous = values.putIfAbsent(property.key, property.value)
				references.putIfAbsent(property.key, property)
				if (previous >= 0 && previous != property.value) return PreparedObservation(id, false)
			}
		}
		return PreparedObservation(id, true, facts.toTypedArray(), references.values.toTypedArray(), children, selected)
	}

	private fun accept(node: PreparedNode, observation: PreparedObservation): Boolean {
		input.checkpoint()
		return when (node) {
			is PreparedNode.Leaf -> {
				if (!observation.matched) {
					false
				} else {
					if (node.consistency.isNotEmpty()) {
						node.properties.firstOrNull { it.key == node.consistency }?.let {
							properties.putIfAbsent(it.key, it.value)
							propertyReferences.putIfAbsent(it.key, it)
						}
					}
					if (node.effect >= 0) effects.add(PreparedEffect(currentCell, node.effect))
					true
				}
			}

			is PreparedNode.Restricted -> if (!observation.matched) {
				reserveMinimum(node.counter)
				false
			} else {
				reserve(node.counter) && accept(node.child, observation.children[0])
			}

			is PreparedNode.Alternative -> {
				var matched = false
				for (index in observation.children.indices) {
					val child = observation.children[index]
					if (!observation.matched && child.matched) continue
					if (accept(node.children[index], child)) matched = true
				}
				matched
			}

			is PreparedNode.DataHatch -> {
				if (observation.children.isEmpty()) {
					false
				} else if (observation.selected[0] == 0) {
					accept(node.data!!, observation.children[0])
				} else {
					(node.data as? PreparedNode.Restricted)?.let { reserveMinimum(it.counter) }
					accept(node.alternative, observation.children[0])
				}
			}

			is PreparedNode.Pattern -> {
				if (!observation.matched) {
					false
				} else {
					var matched = false
					for (index in observation.children.indices) if (acceptRule(node.rules[index], observation.children[index])) matched = true
					if (matched) node.aliases.forEach { counts.addTo(it, 1) }
					matched
				}
			}

			is PreparedNode.Combined -> {
				var matched = observation.matched
				if (matched) {
					for (index in observation.selected) {
						val previous = io
						if (!accept(node.branches[index], observation.children[index])) {
							matched = false
							break
						}
						io = when {
							previous == -1 || io == -1 -> -1
							previous == 0 -> io
							io == 0 || io == previous -> previous
							else -> -1
						}
					}
				}
				matched
			}

			is PreparedNode.Observed -> {
				if (!observation.matched || !accept(node.expression, observation.children[0])) {
					false
				} else {
					var matched = false
					for (rule in node.rules) {
						if (!rule.limited && matched) break
						if ((!rule.limited || reserve(rule.counter))) {
							metadata(rule)
							matched = true
						}
					}
					if (matched) node.aliases.forEach { counts.addTo(it, 1) }
					matched
				}
			}
		}
	}

	private fun acceptRule(rule: PreparedRule, observation: PreparedObservation): Boolean {
		if (!observation.matched) {
			if (rule.limited) reserveMinimum(rule.counter)
			accept(rule.source, observation)
			return false
		}
		if (rule.limited && !reserve(rule.counter)) return false
		if (!accept(rule.source, observation)) return false
		metadata(rule)
		return true
	}

	private fun metadata(rule: PreparedRule) {
		if (rule.io != 0) {
			io = if (io == 0) {
				rule.io
			} else if (io != rule.io) {
				-1
			} else {
				io
			}
		}
		if (rule.renderMask || rule.slot != null) effects.add(PreparedEffect(currentCell, renderMask = rule.renderMask, slot = rule.slot))
	}

	private fun capacity(id: Int): Boolean {
		if (id < 0) return true
		val counter = input.counters[id]
		return (counter.maximum < 0 || maxOf(0, global[id]) < counter.maximum) && (counter.layerMaximum < 0 || maxOf(0, layer[id]) < counter.layerMaximum)
	}

	private fun reserveMinimum(id: Int) {
		if (id < 0) return
		val counter = input.counters[id]
		if (counter.minimum >= 0 && global[id] < 0) global[id] = 0
		if (counter.layerMinimum >= 0 && layer[id] < 0) layer[id] = 0
	}

	private fun reserve(id: Int): Boolean {
		if (id < 0) return true
		val counter = input.counters[id]
		if (counter.minimum >= 0 || counter.maximum >= 0) {
			global[id] = maxOf(global[id], 0) + 1
			if (counter.maximum >= 0 && global[id] > counter.maximum) return false
		}
		if (counter.layerMinimum >= 0 || counter.layerMaximum >= 0) {
			layer[id] = maxOf(layer[id], 0) + 1
			if (counter.layerMaximum >= 0 && layer[id] > counter.layerMaximum) return false
		}
		return true
	}

	private fun minimums(values: IntArray, byLayer: Boolean): Boolean {
		for (index in values.indices) {
			val minimum = if (byLayer) input.counters[index].layerMinimum else input.counters[index].minimum
			if (values[index] in 0..<minimum) {
				problem(currentCell, if (byLayer) "layer minimum" else "minimum count", difference = minimum - values[index], kind = PreparedProblemKind.MINIMUM, counter = index, scope = if (byLayer) PreparedCountScope.LAYER else PreparedCountScope.GLOBAL, limit = minimum, actual = values[index])
				return false
			}
		}
		return true
	}

	private fun constraint(requirement: PreparedRequirement, scope: String, count: Int) {
		input.checkpoint()
		if (problems.isNotEmpty()) return
		if (count in requirement.minimum..requirement.maximum) return
		val cellIndex = input.cells.indexOfFirst { it.scopes.contains(scope) }
		val instance = input.initialScopes.firstOrNull { it.name == scope }
		val cell = input.cells.getOrNull(cellIndex)
		problems.add(
			PreparedPatternProblem(
				cellIndex, scope.substringAfter('@', "$/constraint:$scope"), cell?.repeatPath ?: instance?.repeatPath ?: "",
				cell?.depth ?: instance?.depth ?: 0, cell?.height ?: instance?.height ?: 0, cell?.width ?: instance?.width ?: 0,
				cell?.position ?: instance?.position ?: input.center, requirement.message, requirement.fact,
				if (count < requirement.minimum) requirement.minimum - count else requirement.maximum - count,
				PreparedProblemKind.CONSTRAINT, limit = if (count < requirement.minimum) requirement.minimum else requirement.maximum, actual = count,
			),
		)
	}

	private fun problem(index: Int, expected: String, fact: String = "", difference: Int = 0, kind: PreparedProblemKind = PreparedProblemKind.PREDICATE, counter: Int = -1, scope: PreparedCountScope = PreparedCountScope.GLOBAL, limit: Int = 0, actual: Int = 0) {
		if (problems.isNotEmpty()) return
		val cell = input.cells.getOrNull(index)
		problems.add(PreparedPatternProblem(index, cell?.path ?: "$", cell?.repeatPath ?: "", cell?.depth ?: 0, cell?.height ?: 0, cell?.width ?: 0, cell?.position ?: input.center, expected, fact, difference, kind, counter, scope, limit, actual))
	}

	private fun failed(node: PreparedNode): PreparedObservation = PreparedObservation(node.id, false)
}
