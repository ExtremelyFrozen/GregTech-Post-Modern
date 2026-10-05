package com.gregtechceu.gtceu.api.multiblock.pattern.compile

import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternBinding
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternConstraint
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternDefinition
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternFragment
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternNode
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternParameter

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import org.jspecify.annotations.NullMarked

import java.util.ArrayDeque

@NullMarked
class PatternCompiler {

	fun compile(definition: PatternDefinition): CompiledPattern {
		val cells = ObjectArrayList<CompiledPattern.Cell>()
		val nodeCounts = Object2IntOpenHashMap<String>()
		val nodeIds = ObjectOpenHashSet<String>()
		val fragmentStack = ArrayDeque<String>()
		val depth = validateNode(definition, definition.body, "$", emptyMap(), fragmentStack, nodeIds, nodeCounts, cells, 0)
		validateConstraints(definition, nodeIds)
		return CompiledPattern(definition, cells, nodeCounts, depth)
	}

	private fun validateNode(
		definition: PatternDefinition,
		node: PatternNode,
		path: String,
		bindings: Map<String, PatternBinding>,
		fragmentStack: ArrayDeque<String>,
		nodeIds: MutableSet<String>,
		nodeCounts: Object2IntOpenHashMap<String>,
		cells: MutableList<CompiledPattern.Cell>,
		recursionDepth: Int,
	): Int {
		if (recursionDepth > MAX_RECURSION_DEPTH) {
			throw PatternCompileException("Pattern recursion depth exceeds $MAX_RECURSION_DEPTH at $path")
		}
		return when (node) {
			is PatternNode.Fixed -> {
				validateSymbols(definition, node, path)
				cells.add(CompiledPattern.Cell(path, node, 0, 0, 0))
				nodeCounts.addTo(path, 1)
				recursionDepth
			}

			is PatternNode.Sequence -> node.children.indices.maxOfOrNull { index ->
				validateNode(
					definition, node.children[index], "$path/sequence[$index]", bindings, fragmentStack,
					nodeIds, nodeCounts, cells, recursionDepth,
				)
			} ?: recursionDepth

			is PatternNode.Repeat -> {
				registerNodeId(nodeIds, node.id, path)
				val depth = validateNode(
					definition, node.body, "$path/repeat:${node.id}", bindings, fragmentStack,
					nodeIds, nodeCounts, cells, recursionDepth,
				)
				nodeCounts.addTo("repeat:${node.id}", node.maximum)
				depth
			}

			is PatternNode.Choice -> {
				registerNodeId(nodeIds, node.id, path)
				val alternatives = ObjectOpenHashSet<String>()
				node.alternatives.indices.maxOfOrNull { index ->
					val alternative = node.alternatives[index]
					if (!alternatives.add(alternative.id)) {
						throw PatternCompileException("Duplicate choice alternative '${alternative.id}' at $path")
					}
					validateNode(
						definition, alternative.node, "$path/choice:${node.id}/${alternative.id}", bindings,
						fragmentStack, nodeIds, nodeCounts, cells, recursionDepth,
					)
				} ?: recursionDepth
			}

			is PatternNode.Fragment -> {
				val fragment = definition.fragments[node.id]
					?: throw PatternCompileException("Unknown fragment '${node.id}' at $path")
				if (fragmentStack.contains(node.id)) {
					throw PatternCompileException("Fragment cycle at $path: $fragmentStack")
				}
				validateBindings(fragment, node.bindings, path)
				fragmentStack.push(node.id)
				val depth = validateNode(
					definition, fragment.body, "$path/fragment:${node.id}", node.bindings,
					fragmentStack, nodeIds, nodeCounts, cells, recursionDepth + 1,
				)
				fragmentStack.pop()
				depth
			}
		}
	}

	private fun validateSymbols(definition: PatternDefinition, fixed: PatternNode.Fixed, path: String) {
		fixed.layers.forEach { layer ->
			layer.forEach { row ->
				row.forEach { symbol ->
					if (symbol != ' ' && symbol != '#' && symbol != '@' && !definition.predicates.containsKey(symbol)) {
						throw PatternCompileException("Unknown predicate symbol '$symbol' at $path")
					}
				}
			}
		}
	}

	private fun validateBindings(fragment: PatternFragment, bindings: Map<String, PatternBinding>, path: String) {
		fragment.parameters.forEach { (name, parameter) ->
			val binding = bindings[name] ?: throw PatternCompileException("Missing binding '$name' at $path")
			if (!compatible(parameter, binding)) throw PatternCompileException("Binding '$name' has the wrong type at $path")
		}
		bindings.keys.forEach { name ->
			if (!fragment.parameters.containsKey(name)) throw PatternCompileException("Unknown fragment parameter '$name' at $path")
		}
	}

	private fun compatible(parameter: PatternParameter, binding: PatternBinding): Boolean = when {
		parameter is PatternParameter.Token && binding is PatternBinding.Token -> true

		parameter is PatternParameter.Predicate && binding is PatternBinding.Predicate -> true

		parameter is PatternParameter.IntegerRange && binding is PatternBinding.IntegerValue ->
			binding.value in parameter.minimum..parameter.maximum

		parameter is PatternParameter.Direction && binding is PatternBinding.Direction -> true

		parameter is PatternParameter.Fragment && binding is PatternBinding.Fragment -> true

		else -> false
	}

	private fun registerNodeId(ids: MutableSet<String>, id: String, path: String) {
		if (!ids.add(id)) throw PatternCompileException("Duplicate node id '$id' at $path")
	}

	private fun validateConstraints(definition: PatternDefinition, nodeIds: Set<String>) {
		definition.constraints.forEach { constraint ->
			if (constraint is PatternConstraint.Count) {
				when (val scope = constraint.scope) {
					is PatternConstraint.Scope.Node -> if (!nodeIds.contains(scope.id)) throw PatternCompileException("Unknown node constraint scope '${scope.id}'")
					is PatternConstraint.Scope.Repeat -> if (!nodeIds.contains(scope.id)) throw PatternCompileException("Unknown repeat constraint scope '${scope.id}'")
					is PatternConstraint.Scope.Fragment -> if (!definition.fragments.containsKey(scope.id)) throw PatternCompileException("Unknown fragment constraint scope '${scope.id}'")
					is PatternConstraint.Scope.All -> Unit
				}
			}
		}
	}

	companion object {
		const val MAX_RECURSION_DEPTH: Int = 64
	}
}
