package com.gregtechceu.gtceu.api.multiblock.pattern.layout

import com.gregtechceu.gtceu.api.multiblock.pattern.compile.PatternCompileException
import com.gregtechceu.gtceu.api.multiblock.pattern.fragment.FragmentRegistry
import com.gregtechceu.gtceu.api.multiblock.pattern.model.*
import com.gregtechceu.gtceu.api.multiblock.pattern.predicate.PatternPredicate
import com.gregtechceu.gtceu.api.multiblock.pattern.predicate.PatternPredicateCompiler
import com.gregtechceu.gtceu.api.multiblock.pattern.predicate.PatternPredicates
import com.gregtechceu.gtceu.api.multiblock.pattern.predicate.PredicateController

import net.minecraft.resources.ResourceLocation

import it.unimi.dsi.fastutil.chars.Char2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.chars.Char2ObjectMap
import it.unimi.dsi.fastutil.chars.CharOpenHashSet
import it.unimi.dsi.fastutil.chars.CharSet
import it.unimi.dsi.fastutil.objects.Object2IntMap
import it.unimi.dsi.fastutil.objects.Object2IntMaps
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
import it.unimi.dsi.fastutil.objects.ObjectList
import it.unimi.dsi.fastutil.objects.ObjectLists
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import it.unimi.dsi.fastutil.objects.ObjectSet

import java.util.concurrent.ConcurrentHashMap

/** Resolves recursive structure declarations once; each expansion owns its mutable scratch data. */
class PatternLayoutResolver(private val definition: PatternDefinition) {
	private val repeats = ObjectArrayList<PatternRepeatDescriptor>()
	private val choices = ObjectArrayList<PatternChoiceDescriptor>()
	private val rootSymbols = compileSymbols(definition.predicates)
	private val localByDeclaration = ConcurrentHashMap<String, Char2ObjectMap<PatternPredicate>>()
	var maximumDepth: Int = 0
		private set

	init {
		describe(definition.body, "$", false, ObjectOpenHashSet(), 0)
		validateSymbols(
			definition.body,
			"$",
			CharOpenHashSet().also {
				it.addAll(definition.predicates.keys)
				it.add(' ')
				it.add('#')
				it.add('@')
			},
			0,
		)
		definition.constraints.forEach { constraint ->
			if (constraint is PatternConstraint.Count) {
				when (val scope = constraint.scope) {
					is PatternConstraint.Scope.All -> Unit
					is PatternConstraint.Scope.Repeat -> require(repeats.any { it.id == scope.id }) { "Unknown repeat scope ${scope.id}" }
					is PatternConstraint.Scope.Node -> require(repeats.any { it.id == scope.id } || choices.any { it.id == scope.id }) { "Unknown node scope ${scope.id}" }
					is PatternConstraint.Scope.Fragment -> fragment(scope.id)
				}
			}
		}
	}

	fun repeatDescriptors(): ObjectList<PatternRepeatDescriptor> = ObjectLists.unmodifiable(repeats)
	fun choiceDescriptors(): ObjectList<PatternChoiceDescriptor> = ObjectLists.unmodifiable(choices)

	/** Includes inactive branches and zero-minimum repeats for configuration and candidate discovery. */
	fun allPredicates(): ObjectList<PatternPredicate> {
		val result = ObjectArrayList<PatternPredicate>()
		fun visit(node: PatternNode, symbols: Char2ObjectMap<PatternPredicate>, level: Int) {
			require(level <= 64) { "Pattern nesting exceeds 64" }
			when (node) {
				is PatternNode.Fixed -> node.layers.forEach { layer -> layer.forEach { row -> row.forEach { symbol -> result.add(symbols[symbol] ?: error("Unknown local token $symbol")) } } }

				is PatternNode.Sequence -> node.children.forEach { visit(it, symbols, level + 1) }

				is PatternNode.Repeat -> visit(node.body, symbols, level + 1)

				is PatternNode.Choice -> node.alternatives.forEach { visit(it.node, symbols, level + 1) }

				is PatternNode.Placed -> visit(node.body, symbols, level + 1)

				is PatternNode.Fragment -> {
					val target = fragment(node.id)
					visit(bind(target.body, node.bindings), localSymbols(target, node, symbols), level + 1)
				}
			}
		}
		visit(definition.body, rootSymbols, 0)
		return ObjectLists.unmodifiable(ObjectArrayList(ObjectLinkedOpenHashSet(result)))
	}

	private fun localSymbols(target: PatternFragment, reference: PatternNode.Fragment, caller: Char2ObjectMap<PatternPredicate>): Char2ObjectMap<PatternPredicate> {
		val local = Char2ObjectLinkedOpenHashMap<PatternPredicate>()
		local[' '] = PatternPredicates.any()
		local['#'] = PatternPredicates.air()
		fun predicate(name: String): PatternPredicate = when (val binding = reference.bindings[name] ?: error("Unknown parameter $name")) {
			is PatternBinding.Token -> caller[binding.value.single()] ?: error("Unknown caller token ${binding.value}")
			is PatternBinding.Predicate -> binding.definition?.let(PatternPredicateCompiler::compile) ?: caller[binding.value.single()] ?: error("Unknown caller predicate ${binding.value}")
			else -> error("Parameter $name is not a predicate")
		}
		fun bound(value: Any): Any = when {
			value is String && value.startsWith("$") -> when (val binding = reference.bindings[value.substring(1)] ?: error("Unknown parameter $value")) {
				is PatternBinding.IntegerValue -> binding.value
				is PatternBinding.Direction -> binding.value.name.lowercase()
				is PatternBinding.Token -> binding.value
				is PatternBinding.Fragment -> binding.value
				is PatternBinding.Predicate -> binding.value
			}

			value is Map<*, *> -> Object2ObjectLinkedOpenHashMap<String, Any>().also { out -> value.forEach { (key, child) -> out[key.toString()] = bound(requireNotNull(child)) } }

			value is List<*> -> ObjectArrayList<Any>().also { out -> value.forEach { out.add(bound(requireNotNull(it))) } }

			else -> value
		}
		target.parameters.forEach { (name, parameter) -> if (name.length == 1 && (parameter is PatternParameter.Token || parameter is PatternParameter.Predicate)) local[name[0]] = predicate(name) }
		target.predicates.forEach { (symbol, source) ->
			local[symbol] =
				if (source.type == "parameter") predicate(source.properties["parameter"].toString()) else PatternPredicateCompiler.compile(source.copy(properties = Object2ObjectLinkedOpenHashMap<String, Any>().also { out -> source.properties.forEach { (key, value) -> out[key] = bound(value) } }))
		}
		return local
	}

	fun expand(repetitions: IntArray, choices: Map<String, String>): PatternLayout = expand(repetitions, choices, Object2IntMaps.emptyMap())

	fun expand(repetitions: IntArray, choices: Map<String, String>, instanceRepetitions: Object2IntMap<String>): PatternLayout {
		val selected = Object2ObjectLinkedOpenHashMap(choices)
		instanceRepetitions.object2IntEntrySet().forEach { selected["${it.key}#repeat"] = it.intValue.toString() }
		val layout = expandResolved(repetitions, selected)
		choices.forEach { (key, branch) ->
			val descriptor = this.choices.firstOrNull { it.path == key || it.id == key } ?: layout.choices.firstOrNull { it.path == key }?.descriptor ?: error("Unknown choice '$key'")
			require(descriptor.alternatives.contains(branch)) { "Unknown branch '$branch' for '$key'" }
		}
		instanceRepetitions.object2IntEntrySet().forEach { entry -> require(layout.repeats.any { it.path == entry.key }) { "Unknown repeat instance '${entry.key}'" } }
		return layout
	}

	internal fun expandResolved(repetitions: IntArray, selected: Map<String, String>): PatternLayout {
		require(repetitions.size == repeats.size) { "Expected ${repeats.size} repetition counts, got ${repetitions.size}" }
		repeats.forEach { require(repetitions[it.index] in it.minimum..it.maximum) { "Repetition outside bounds at ${it.path}" } }
		val root = rootSymbols
		val box = expandNode(definition.body, "$", "$", root, emptyMap(), repetitions, selected, ObjectArrayList(), "", -1, 0, 0)
		val origin = definition.origin
		val controllers = box.cells.filter { it.predicate is PredicateController || (origin.token != "controller" && it.token == origin.token) }
		val center = if (origin.token == "offset") {
			intArrayOf(origin.offset.depth, origin.offset.height, origin.offset.width)
		} else {
			require(controllers.size == 1) { "Pattern requires exactly one origin token '${origin.token}', found ${controllers.size}" }
			controllers.single().let { intArrayOf(it.depth + origin.offset.depth, it.height + origin.offset.height, it.width + origin.offset.width) }
		}
		val cells = ObjectArrayList<PatternLayoutCell>(box.cells.size)
		box.cells.forEach { cells.add(it.copy(depth = it.depth - center[0], height = it.height - center[1], width = it.width - center[2])) }
		val instances = ObjectArrayList<PatternRepeatInstance>(box.repeats.size)
		box.repeats.forEach { instances.add(it.copy(depth = it.depth - center[0], height = it.height - center[1], width = it.width - center[2])) }
		return PatternLayout(ObjectLists.unmodifiable(cells), ObjectLists.unmodifiable(instances), ObjectLists.unmodifiable(box.choices))
	}

	private fun fragment(id: String): PatternFragment = definition.fragments[id]
		?: FragmentRegistry.require(ResourceLocation.parse(id))

	private fun describe(node: PatternNode, path: String, nested: Boolean, stack: ObjectSet<String>, level: Int) {
		require(level <= 64) { "Pattern nesting exceeds 64 at $path" }
		maximumDepth = maxOf(maximumDepth, level)
		when (node) {
			is PatternNode.Fixed -> if (!nested) repeats.add(PatternRepeatDescriptor(repeats.size, path, path, PatternAxis.DEPTH, 1, 1))

			is PatternNode.Sequence -> node.children.forEachIndexed { i, child -> describe(child, "$path/sequence[$i]", nested, stack, level + 1) }

			is PatternNode.Repeat -> {
				repeats.add(PatternRepeatDescriptor(repeats.size, node.id, path, PatternAxis.of(node.axis), node.minimum, node.maximum))
				describe(node.body, "$path/repeat:${node.id}", true, stack, level + 1)
			}

			is PatternNode.Choice -> {
				val ids = ObjectArrayList<String>()
				node.alternatives.forEach {
					require(!ids.contains(it.id)) { "Duplicate branch at $path" }
					ids.add(it.id)
				}
				choices.add(PatternChoiceDescriptor(node.id, path, ObjectLists.unmodifiable(ids), ObjectLists.unmodifiable(ObjectArrayList(node.priority))))
				node.alternatives.forEach { describe(it.node, "$path/choice:${node.id}/${it.id}", true, stack, level + 1) }
			}

			is PatternNode.Fragment -> {
				require(stack.add(node.id)) { "Fragment cycle at $path: ${node.id}" }
				describe(bind(fragment(node.id).body, node.bindings), "$path/fragment:${node.id}", nested, stack, level + 1)
				stack.remove(node.id)
			}

			is PatternNode.Placed -> describe(node.body, "$path/placed", nested, stack, level + 1)
		}
	}

	private fun compileSymbols(source: Char2ObjectMap<PatternPredicateDefinition>): Char2ObjectMap<PatternPredicate> {
		val result = Char2ObjectLinkedOpenHashMap<PatternPredicate>()
		result[' '] = PatternPredicates.any()
		result['#'] = PatternPredicates.air()
		result['@'] = PatternPredicateCompiler.controller(definition.machine)
		source.forEach { (symbol, value) -> result[symbol] = PatternPredicateCompiler.compile(value) }
		return result
	}

	private fun bind(node: PatternNode, values: Map<String, PatternBinding>): PatternNode = when (node) {
		is PatternNode.Fixed -> node

		is PatternNode.Sequence -> node.copy(children = ObjectArrayList<PatternNode>().also { out -> node.children.forEach { out.add(bind(it, values)) } })

		is PatternNode.Repeat -> node.copy(
			minimum = if (node.minimumParameter.isEmpty()) node.minimum else (values[node.minimumParameter] as? PatternBinding.IntegerValue)?.value ?: error("Missing integer ${node.minimumParameter}"),
			maximum = if (node.maximumParameter.isEmpty()) node.maximum else (values[node.maximumParameter] as? PatternBinding.IntegerValue)?.value ?: error("Missing integer ${node.maximumParameter}"),
			axis = if (node.axisParameter.isEmpty()) node.axis else (values[node.axisParameter] as? PatternBinding.Direction)?.value ?: error("Missing direction ${node.axisParameter}"),
			body = bind(node.body, values),
			minimumParameter = "",
			maximumParameter = "",
			axisParameter = "",
		)

		is PatternNode.Choice -> node.copy(alternatives = ObjectArrayList<PatternNode.Choice.Alternative>().also { out -> node.alternatives.forEach { out.add(it.copy(node = bind(it.node, values))) } })

		is PatternNode.Fragment -> node.copy(
			id = if (node.id.startsWith("$")) (values[node.id.substring(1)] as? PatternBinding.Fragment)?.value ?: error("Missing fragment ${node.id}") else node.id,
			bindings = Object2ObjectLinkedOpenHashMap<String, PatternBinding>().also { out ->
				node.bindings.forEach { (name, binding) ->
					out[name] = if (binding is PatternBinding.Token && binding.value.startsWith("$")) values[binding.value.substring(1)] ?: error("Unknown forwarded parameter ${binding.value}") else binding
				}
			},
		)

		is PatternNode.Placed -> node.copy(body = bind(node.body, values))
	}

	private fun validateSymbols(node: PatternNode, path: String, symbols: CharSet, level: Int) {
		require(level <= 64) { "Pattern nesting exceeds 64 at $path" }
		when (node) {
			is PatternNode.Fixed -> node.layers.forEach { layer -> layer.forEach { row -> row.forEach { require(symbols.contains(it)) { "Undeclared local token '$it' at $path" } } } }

			is PatternNode.Sequence -> node.children.forEachIndexed { i, child -> validateSymbols(child, "$path/sequence[$i]", symbols, level + 1) }

			is PatternNode.Repeat -> validateSymbols(node.body, "$path/repeat:${node.id}", symbols, level + 1)

			is PatternNode.Choice -> node.alternatives.forEach { validateSymbols(it.node, "$path/choice:${node.id}/${it.id}", symbols, level + 1) }

			is PatternNode.Placed -> validateSymbols(node.body, "$path/placed", symbols, level + 1)

			is PatternNode.Fragment -> {
				val target = fragment(node.id)
				require(node.bindings.keys == target.parameters.keys) { "Fragment parameter names do not match at $path" }
				val local = CharOpenHashSet()
				local.add(' ')
				local.add('#')
				local.addAll(target.predicates.keys)
				target.parameters.forEach { (name, parameter) ->
					val value = node.bindings[name]
					when (parameter) {
						is PatternParameter.Token, is PatternParameter.Predicate -> {
							if (value is PatternBinding.Predicate && value.definition != null) {
								if (name.length == 1) local.add(name[0])
								return@forEach
							}
							val token = when (value) {
								is PatternBinding.Token -> value.value
								is PatternBinding.Predicate -> value.value
								else -> error("Wrong binding type for $name at $path")
							}
							require(token.length == 1 && symbols.contains(token[0])) { "Unknown caller token '$token' at $path" }
							if (name.length == 1) local.add(name[0])
						}

						is PatternParameter.IntegerRange -> require(value is PatternBinding.IntegerValue && value.value in parameter.minimum..parameter.maximum) { "Integer parameter '$name' outside bounds at $path" }

						is PatternParameter.Direction -> require(value is PatternBinding.Direction) { "Wrong direction binding at $path" }

						is PatternParameter.Fragment -> require(value is PatternBinding.Fragment) { "Wrong fragment binding at $path" }
					}
				}
				validateSymbols(bind(target.body, node.bindings), "$path/fragment:${node.id}", local, level + 1)
			}
		}
	}

	private data class Box(val cells: ObjectArrayList<PatternLayoutCell>, val minimum: IntArray, val maximum: IntArray, val repeats: ObjectArrayList<PatternRepeatInstance> = ObjectArrayList(), val choices: ObjectArrayList<PatternChoiceInstance> = ObjectArrayList(), val placed: Boolean = false) {
		fun size(axis: Int): Int = Math.subtractExact(maximum[axis], minimum[axis])
	}

	private fun expandNode(
		node: PatternNode,
		declaration: String,
		path: String,
		symbols: Char2ObjectMap<PatternPredicate>,
		bindings: Map<String, PatternBinding>,
		repetitions: IntArray,
		selected: Map<String, String>,
		scopes: ObjectList<String>,
		repeatPath: String,
		unit: Int,
		repetition: Int,
		level: Int,
	): Box {
		require(level <= 64) { "Pattern nesting exceeds 64 at $path" }
		val descriptor = repeats.firstOrNull { it.path == declaration }
		val currentUnit = if (unit < 0 && descriptor != null) descriptor.index else unit
		return when (node) {
			is PatternNode.Fixed -> {
				val cells = ObjectArrayList<PatternLayoutCell>()
				node.layers.forEachIndexed { d, layer ->
					layer.forEachIndexed { h, row ->
						row.forEachIndexed { w, symbol ->
							val predicate = symbols[symbol] ?: throw PatternCompileException("Undeclared local token '$symbol' at $path")
							cells.add(PatternLayoutCell("$path/[$d,$h,$w]", repeatPath, d, h, w, predicate, symbol.toString(), ObjectArrayList(scopes), currentUnit.coerceAtLeast(0), repetition, d, h, w, declaration))
						}
					}
				}
				Box(cells, IntArray(3), intArrayOf(node.depth(), node.height(), node.width()))
			}

			is PatternNode.Sequence -> {
				val children = ObjectArrayList<Box>()
				node.children.forEachIndexed { i, child -> children.add(expandNode(child, "$declaration/sequence[$i]", "$path/sequence[$i]", symbols, bindings, repetitions, selected, scopes, repeatPath, currentUnit, repetition, level + 1)) }
				join(children, PatternAxis.of(node.axis).ordinal, false)
			}

			is PatternNode.Repeat -> {
				val declared = requireNotNull(descriptor)
				val count = selected["$path#repeat"]?.toInt() ?: repetitions[declared.index]
				require(count in node.minimum..node.maximum) { "Instance repetition outside bounds at $path" }
				val children = ObjectArrayList<Box>()
				repeat(count) { i ->
					val instance = "$path/repeat:${node.id}[$i]"
					val childScopes = ObjectArrayList(scopes)
					childScopes.add("node:${node.id}@$instance")
					childScopes.add("repeat:${node.id}@$path")
					children.add(expandNode(node.body, "$declaration/repeat:${node.id}", instance, symbols, bindings, repetitions, selected, childScopes, instance, currentUnit, i, level + 1))
				}
				join(children, PatternAxis.of(node.axis).ordinal, node.direction == PatternRepeatDirection.NEGATIVE).also { it.repeats.add(PatternRepeatInstance(path, declared)) }
			}

			is PatternNode.Choice -> {
				val id = selected[path] ?: selected[declaration] ?: selected[node.id] ?: node.priority.firstOrNull() ?: node.alternatives.first().id
				val branch = node.alternatives.firstOrNull { it.id == id } ?: error("Unknown branch '$id' at $path")
				val childScopes = ObjectArrayList(scopes)
				childScopes.add("node:${node.id}@$path")
				expandNode(branch.node, "$declaration/choice:${node.id}/${branch.id}", "$path/choice:${node.id}/${branch.id}", symbols, bindings, repetitions, selected, childScopes, repeatPath, currentUnit, repetition, level + 1).also {
					it.choices.add(
						PatternChoiceInstance(
							path,
							choices.first { choice ->
								choice.path ==
									declaration
							},
						),
					)
				}
			}

			is PatternNode.Fragment -> {
				val targetId = (bindings[node.id.removePrefix("$")] as? PatternBinding.Fragment)?.value ?: node.id
				val target = fragment(targetId)
				val local = localByDeclaration.computeIfAbsent(declaration) { localSymbols(target, node, symbols) }
				val childScopes = ObjectArrayList(scopes)
				childScopes.add("fragment:$targetId@$path")
				expandNode(bind(target.body, node.bindings), "$declaration/fragment:${node.id}", "$path/fragment:$targetId", local, node.bindings, repetitions, selected, childScopes, repeatPath, currentUnit, repetition, level + 1)
			}

			is PatternNode.Placed -> {
				val child = expandNode(node.body, "$declaration/placed", "$path/placed", symbols, bindings, repetitions, selected, scopes, repeatPath, currentUnit, repetition, level + 1)
				shift(child, intArrayOf(node.offset.depth, node.offset.height, node.offset.width)).copy(placed = true)
			}
		}
	}

	private fun join(children: ObjectList<Box>, axis: Int, negative: Boolean): Box {
		val crossSize = IntArray(3)
		children.forEach { box -> if (!box.placed) for (i in 0..2) crossSize[i] = maxOf(crossSize[i], box.size(i)) }
		val cells = ObjectArrayList<PatternLayoutCell>()
		val repeatInstances = ObjectArrayList<PatternRepeatInstance>()
		val choiceInstances = ObjectArrayList<PatternChoiceInstance>()
		val minimum = IntArray(3) { Int.MAX_VALUE }
		val maximum = IntArray(3) { Int.MIN_VALUE }
		var cursor = 0
		children.forEach { box ->
			val offset = IntArray(3) { i ->
				if (box.placed) {
					0
				} else if (i == axis) {
					if (negative) Math.subtractExact(-cursor, box.maximum[i]) else Math.subtractExact(cursor, box.minimum[i])
				} else if (box.cells.isEmpty()) {
					0
				} else {
					require((crossSize[i] - box.size(i)) % 2 == 0) { "Cannot center nodes of incompatible parity; place the node with an explicit offset" }
					Math.subtractExact((crossSize[i] - box.size(i)) / 2, box.minimum[i])
				}
			}
			val shifted = shift(box, offset)
			cells.addAll(shifted.cells)
			repeatInstances.addAll(shifted.repeats)
			choiceInstances.addAll(shifted.choices)
			for (i in 0..2) {
				minimum[i] = minOf(minimum[i], shifted.minimum[i])
				maximum[i] = maxOf(maximum[i], shifted.maximum[i])
			}
			if (!box.placed) cursor = Math.addExact(cursor, box.size(axis))
		}
		return Box(cells, if (children.isEmpty()) IntArray(3) else minimum, if (children.isEmpty()) IntArray(3) else maximum, repeatInstances, choiceInstances)
	}

	private fun shift(box: Box, offset: IntArray): Box {
		val cells = ObjectArrayList<PatternLayoutCell>(box.cells.size)
		box.cells.forEach { cells.add(it.copy(depth = Math.addExact(it.depth, offset[0]), height = Math.addExact(it.height, offset[1]), width = Math.addExact(it.width, offset[2]))) }
		val repeats = ObjectArrayList<PatternRepeatInstance>(box.repeats.size)
		box.repeats.forEach { repeats.add(it.copy(depth = Math.addExact(it.depth, offset[0]), height = Math.addExact(it.height, offset[1]), width = Math.addExact(it.width, offset[2]))) }
		return Box(cells, IntArray(3) { Math.addExact(box.minimum[it], offset[it]) }, IntArray(3) { Math.addExact(box.maximum[it], offset[it]) }, repeats, box.choices, box.placed)
	}
}
