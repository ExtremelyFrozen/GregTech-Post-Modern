package com.gregtechceu.gtceu.api.multiblock.pattern.preparation

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet

/** A successful group's detached contribution has no back-reference to its original input or world. */
internal data class PreparedGroupContribution(
	val indices: IntArray,
	val accepted: Array<PreparedAcceptedCell>,
	val effects: Array<PreparedEffect>,
	val factCounts: Array<PreparedFactCount>,
	val scopes: Array<String>,
	val scopedCounts: Array<PreparedScopedCount>,
	val properties: Array<PreparedProperty>,
	val counterIds: IntArray,
	val globalCounts: IntArray,
	val layerCounts: IntArray,
)

internal data class PreparedDependencyGroup(val indices: IntArray, val cached: PreparedGroupContribution?)
internal data class PreparedGroupPlan(val groups: Array<PreparedDependencyGroup>)

/** Union operations are conservative: all rule alternatives at one cell are one indivisible computation. */
internal object PreparedDependencyGroups {
	fun build(cells: Array<PreparedCell>, dependencies: Array<LongArray>, requirements: Array<PreparedRequirement>, changed: BooleanArray, previous: Array<PreparedGroupContribution>?): PreparedGroupPlan {
		val parent = IntArray(cells.size) { it }
		fun root(start: Int): Int {
			var node = start
			while (parent[node] != node) {
				parent[node] = parent[parent[node]]
				node = parent[node]
			}
			return node
		}
		fun union(first: Int, second: Int) {
			val left = root(first)
			val right = root(second)
			if (left != right) parent[right] = left
		}
		val counters = Int2IntOpenHashMap().also { it.defaultReturnValue(-1) }
		val properties = Object2IntOpenHashMap<String>().also { it.defaultReturnValue(-1) }
		val aggregate = Object2IntOpenHashMap<String>().also { it.defaultReturnValue(-1) }
		val constraints = Object2IntOpenHashMap<String>().also { it.defaultReturnValue(-1) }
		val byPosition = Long2IntOpenHashMap().also { it.defaultReturnValue(-1) }
		fun connect(table: Object2IntOpenHashMap<String>, key: String, index: Int) {
			val previousIndex = table.putIfAbsent(key, index)
			if (previousIndex >= 0) union(previousIndex, index)
		}
		val resources = Resources()
		for (index in cells.indices) {
			for (position in dependencies[index]) {
				val previousIndex = byPosition.putIfAbsent(position, index)
				if (previousIndex >= 0) union(previousIndex, index)
			}
			resources.clear()
			resources.visit(cells[index].root)
			val counterIterator = resources.counters.iterator()
			while (counterIterator.hasNext()) {
				val counter = counterIterator.nextInt()
				val previousIndex = counters.putIfAbsent(counter, index)
				if (previousIndex >= 0) union(previousIndex, index)
			}
			for (property in resources.properties) connect(properties, property, index)
			for (key in resources.aggregates) connect(aggregate, key, index)
			resources.facts.add("token:${cells[index].token}")
			for ((fact, prefix) in requirements) {
				if (resources.facts.contains(fact)) {
					if (prefix == "all") {
						connect(constraints, "all\u0000$fact", index)
					} else {
						for (scope in cells[index].scopes) if (scope.startsWith("$prefix@")) connect(constraints, "$scope\u0000$fact", index)
					}
				}
			}
		}
		val members = Int2IntOpenHashMap().also { it.defaultReturnValue(-1) }
		val groups = ObjectArrayList<IntArrayList>()
		for (index in cells.indices) {
			val representative = root(index)
			var group = members.get(representative)
			if (group < 0) {
				group = groups.size
				members[representative] = group
				groups.add(IntArrayList())
			}
			groups[group].add(index)
		}
		val oldByFirst = Int2IntOpenHashMap().also { it.defaultReturnValue(-1) }
		if (previous != null) {
			for (index in previous.indices) {
				val indices = previous[index].indices
				if (indices.isNotEmpty()) oldByFirst.put(indices[0], index)
			}
		}
		return PreparedGroupPlan(
			Array(groups.size) { groupIndex ->
				val membersInGroup = groups[groupIndex]
				val indices = membersInGroup.toIntArray()
				val oldIndex = oldByFirst.get(indices[0])
				val candidate = if (oldIndex >= 0) previous!![oldIndex] else null
				var affected = false
				for (index in indices) {
					if (changed[index]) {
						affected = true
						break
					}
				}
				val cached = if (!affected && candidate != null && candidate.indices.contentEquals(indices)) candidate else null
				PreparedDependencyGroup(indices, cached)
			},
		)
	}

	private class Resources {
		val counters = IntOpenHashSet()
		val properties = ObjectOpenHashSet<String>()
		val aggregates = ObjectOpenHashSet<String>()
		val facts = ObjectOpenHashSet<String>()

		fun clear() {
			counters.clear()
			properties.clear()
			aggregates.clear()
			facts.clear()
		}

		private fun rules(rules: Array<PreparedRule>) {
			for ((source, counter) in rules) {
				if (counter >= 0) counters.add(counter)
				visit(source)
			}
		}

		fun visit(node: PreparedNode) {
			when (node) {
				is PreparedNode.Leaf -> {
					for (fact in node.facts) facts.add(fact)
					for (property in node.properties) properties.add(property.key)
					if (node.consistency.isNotEmpty()) properties.add(node.consistency)
					for (key in node.aggregation) aggregates.add(key)
				}

				is PreparedNode.Pattern -> {
					rules(node.rules)
					for (fact in node.aliases) facts.add(fact)
				}

				is PreparedNode.Observed -> {
					visit(node.expression)
					rules(node.rules)
					for (fact in node.aliases) facts.add(fact)
				}

				is PreparedNode.Alternative -> for (child in node.children) visit(child)

				is PreparedNode.Restricted -> {
					if (node.counter >= 0) counters.add(node.counter)
					visit(node.child)
				}

				is PreparedNode.DataHatch -> {
					node.data?.let(::visit)
					visit(node.alternative)
				}

				is PreparedNode.Combined -> {
					for (branch in node.branches) visit(branch)
					for (fact in node.aliases) facts.add(fact)
				}
			}
		}
	}
}
