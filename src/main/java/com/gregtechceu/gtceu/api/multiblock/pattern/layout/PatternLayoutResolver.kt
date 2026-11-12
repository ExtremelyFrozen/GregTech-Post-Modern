package com.gregtechceu.gtceu.api.multiblock.pattern.layout

import com.gregtechceu.gtceu.api.multiblock.pattern.compile.PatternCompileException
import com.gregtechceu.gtceu.api.multiblock.pattern.fragment.FragmentRegistry
import com.gregtechceu.gtceu.api.multiblock.pattern.match.PatternSelections
import com.gregtechceu.gtceu.api.multiblock.pattern.match.PatternWork
import com.gregtechceu.gtceu.api.multiblock.pattern.model.*
import com.gregtechceu.gtceu.api.multiblock.pattern.model.measurement.PatternMeasurement
import com.gregtechceu.gtceu.api.multiblock.pattern.model.module.PatternModule
import com.gregtechceu.gtceu.api.multiblock.pattern.model.module.PatternModuleSlot
import com.gregtechceu.gtceu.api.multiblock.pattern.model.module.PatternPortBoundary
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.PatternPredicate
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.PatternPredicateCompiler
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.PredicateController

import net.minecraft.resources.ResourceLocation

import it.unimi.dsi.fastutil.chars.Char2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.chars.Char2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap
import it.unimi.dsi.fastutil.objects.*

import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/** Resolves recursive structure declarations once; each expansion owns its mutable scratch data. */
class PatternLayoutResolver(private val definition: PatternDefinition) {
	private val repeats = ObjectArrayList<PatternRepeatDescriptor>()
	private val choices = ObjectArrayList<PatternChoiceDescriptor>()
	private val moduleIds = ObjectOpenHashSet<String>()
	private val rootSymbols = compileSymbols(definition.predicates)
	private data class LocalSymbolKey(val declaration: String, val bindings: Object2ObjectMap<String, PatternBinding>)
	private val localByDeclaration = ConcurrentHashMap<LocalSymbolKey, Char2ObjectMap<PatternPredicate>>()
	private val integerParameterNames = ObjectArrayList<String>().also { names -> for ((name, parameter) in definition.parameters) if (parameter is PatternParameter.IntegerRange) names.add(name) }.toTypedArray()
	private class LayoutKey(repetitions: IntArray, choices: Map<String, String>, names: Array<String>, parameters: Object2IntMap<String>) {
		private val repetitions = repetitions.copyOf()
		private val choiceKeys = choices.keys.toTypedArray().also { it.sort() }
		private val choiceValues = Array(choiceKeys.size) { choices[choiceKeys[it]]!! }
		private val parameterValues = IntArray(names.size) { parameters.getInt(names[it]) }
		private val hash = 31 * (31 * (31 * this.repetitions.contentHashCode() + choiceKeys.contentHashCode()) + choiceValues.contentHashCode()) + parameterValues.contentHashCode()
		override fun hashCode(): Int = hash
		override fun equals(other: Any?): Boolean = other is LayoutKey && hash == other.hash && repetitions.contentEquals(other.repetitions) && choiceKeys.contentEquals(other.choiceKeys) && choiceValues.contentEquals(other.choiceValues) && parameterValues.contentEquals(other.parameterValues)
	}
	private val layouts = Object2ObjectLinkedOpenHashMap<LayoutKey, PatternLayout>()
	private var cachedCellCount = 0
	var maximumDepth: Int = 0
		private set

	init {
		val initial = bind(definition.body, rootBindings(Object2IntMaps.emptyMap()))
		describe(initial, "$", false, ObjectOpenHashSet(), 0)
	}

	fun repeatDescriptors(): ObjectList<PatternRepeatDescriptor> = ObjectLists.unmodifiable(repeats)
	fun choiceDescriptors(): ObjectList<PatternChoiceDescriptor> = ObjectLists.unmodifiable(choices)
	fun measurements(): ObjectList<PatternMeasurement> = ObjectLists.unmodifiable(definition.measurements)

	private fun rootBindings(values: Object2IntMap<String>): Object2ObjectMap<String, PatternBinding> = Object2ObjectLinkedOpenHashMap<String, PatternBinding>().also { out ->
		definition.parameters.forEach { (name, parameter) -> if (parameter is PatternParameter.IntegerRange) out[name] = PatternBinding.IntegerValue(if (values.containsKey(name)) values.getInt(name) else parameter.minimum) }
	}

	private fun integerValues(bindings: Map<String, PatternBinding>): Object2IntMap<String> = PatternBindingContext.integers(bindings)

	private fun resolveBindings(values: Map<String, PatternBinding>, caller: Map<String, PatternBinding>): Object2ObjectMap<String, PatternBinding> = PatternBindingContext.resolve(values, caller)

	/** Conservative distances from any possible origin, without enumerating repeated cells or choice products. */
	fun searchRadius(): IntArray {
		fun extent(node: PatternNode): LongArray = when (node) {
			is PatternNode.Assembly -> {
				fun portAllowance(target: PatternFragment): Long = target.ports.values.sumOf { abs(it.offset.depth.toLong()) + abs(it.offset.height.toLong()) + abs(it.offset.width.toLong()) + 1L }
				fun slotExtent(slots: ObjectList<PatternModuleSlot>): Long = slots.sumOf { slot ->
					val module = slotModules(slot).maxOf { candidate -> extent(PatternNode.Fragment(candidate.fragment, candidate.bindings)).max() + portAllowance(fragment(candidate.fragment)) + slotExtent(candidate.children) }
					Math.multiplyExact(module, if (slot is PatternModuleSlot.Chain) slot.maximum.toLong() else 1L)
				}
				val total = extent(node.root).max() + portAllowance(fragment(node.root.id)) + slotExtent(node.slots)
				LongArray(3) { total }
			}

			is PatternNode.Fixed -> longArrayOf(node.depth().toLong(), node.height().toLong(), node.width().toLong())

			is PatternNode.Sequence -> LongArray(3).also { out -> node.children.forEach { child -> extent(child).forEachIndexed { axis, value -> out[axis] = Math.addExact(out[axis], value) } } }

			is PatternNode.Repeat -> extent(node.body).also { out -> out[PatternAxis.of(node.axis).ordinal] = Math.multiplyExact(out[PatternAxis.of(node.axis).ordinal], node.maximum.toLong()) }

			is PatternNode.Choice -> LongArray(3).also { out -> node.alternatives.forEach { branch -> extent(branch.node).forEachIndexed { axis, value -> out[axis] = maxOf(out[axis], value) } } }

			is PatternNode.Fragment -> extent(bind(fragment(node.id).body, node.bindings))

			is PatternNode.Placed -> extent(node.body).also { out ->
				out[0] += 2L * abs(node.offset.depth.toLong())
				out[1] += 2L * abs(node.offset.height.toLong())
				out[2] += 2L * abs(node.offset.width.toLong())
			}
		}
		val extent = extent(bind(definition.body, rootBindings(Object2IntMaps.emptyMap())))
		val origin = definition.origin.offset
		return intArrayOf(
			minOf(Int.MAX_VALUE.toLong(), extent[0] * 2 + abs(origin.depth.toLong())).toInt(),
			minOf(Int.MAX_VALUE.toLong(), extent[1] * 2 + abs(origin.height.toLong())).toInt(),
			minOf(Int.MAX_VALUE.toLong(), extent[2] * 2 + abs(origin.width.toLong())).toInt(),
		)
	}

	/** Measures the same layout bounds without expanding its cells, predicates or instance metadata. */
	fun dimensions(repetitions: IntArray, choices: Map<String, String>): IntArray = dimensions(repetitions, choices, Object2IntMaps.emptyMap())

	fun dimensions(repetitions: IntArray, choices: Map<String, String>, parameterValues: Object2IntMap<String>): IntArray {
		val bindings = rootBindings(parameterValues)
		val box = PatternWork { expandNode(this, bind(definition.body, bindings), "$", "$", rootSymbols, bindings, repetitions, choices, ObjectArrayList(), "", -1, 0, 0, true) }.finish()
		return IntArray(3) { box.size(it) }
	}

	/** Finds the nearest attainable size for one repeat while preserving the other dimensions and choices. */
	fun resize(repetitions: IntArray, choices: Map<String, String>, index: Int, size: Int): IntArray {
		val descriptor = repeats[index]
		val result = repetitions.copyOf()
		fun sizeAt(amount: Int): Int {
			result[index] = amount
			return dimensions(result, choices)[descriptor.axis.ordinal]
		}
		var lower = descriptor.minimum
		var upper = descriptor.maximum
		while (lower < upper) {
			val middle = lower + (upper - lower) / 2
			if (sizeAt(middle) < size) lower = middle + 1 else upper = middle
		}
		val above = sizeAt(lower)
		val amount = if (lower > descriptor.minimum && size - sizeAt(lower - 1) < above - size) lower - 1 else lower
		result[index] = amount
		return result
	}

	/** Includes inactive branches and zero-minimum repeats for configuration and candidate discovery. */
	fun allPredicates(): ObjectList<PatternPredicate> {
		val result = ObjectArrayList<PatternPredicate>()
		fun visit(node: PatternNode, symbols: Char2ObjectMap<PatternPredicate>, level: Int) {
			when (node) {
				is PatternNode.Assembly -> {
					visit(node.root, symbols, level + 1)
					fun slots(values: ObjectList<PatternModuleSlot>, caller: Char2ObjectMap<PatternPredicate>) {
						for (slot in values) {
							for ((fragment, _, _, bindings, children) in slotModules(slot)) {
								val reference = PatternNode.Fragment(fragment, bindings)
								visit(reference, caller, level + 1)
								slots(children, localSymbols(fragment(fragment), reference, caller))
							}
						}
					}
					slots(node.slots, localSymbols(fragment(node.root.id), node.root, symbols))
				}

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
		visit(bind(definition.body, rootBindings(Object2IntMaps.emptyMap())), rootSymbols, 0)
		return ObjectLists.unmodifiable(ObjectArrayList(ObjectLinkedOpenHashSet(result)))
	}

	private fun localSymbols(target: PatternFragment, reference: PatternNode.Fragment, caller: Char2ObjectMap<PatternPredicate>): Char2ObjectMap<PatternPredicate> {
		val local = Char2ObjectLinkedOpenHashMap<PatternPredicate>()
		local[' '] = PatternPredicate.any()
		local['#'] = PatternPredicate.air()
		fun predicate(name: String): PatternPredicate = when (val binding = reference.bindings[name] ?: error("Unknown parameter $name")) {
			is PatternBinding.Token -> caller[binding.value.single()] ?: error("Unknown caller token ${binding.value}")
			is PatternBinding.Predicate -> binding.definition?.let(PatternPredicateCompiler::compile) ?: caller[binding.value.single()] ?: error("Unknown caller predicate ${binding.value}")
			else -> error("Parameter $name is not a predicate")
		}
		fun bound(value: Any): Any = when (value) {
			is String if value.startsWith("$") -> when (val binding = reference.bindings[value.substring(1)] ?: error("Unknown parameter $value")) {
				is PatternBinding.IntegerValue -> binding.value
				is PatternBinding.IntegerExpression -> error("Unresolved integer predicate binding")
				is PatternBinding.Direction -> binding.value.name.lowercase()
				is PatternBinding.Token -> binding.value
				is PatternBinding.Fragment -> binding.value
				is PatternBinding.Predicate -> binding.value
			}

			is Map<*, *> -> Object2ObjectLinkedOpenHashMap<String, Any>().also { out -> value.forEach { (key, child) -> out[key.toString()] = bound(child!!) } }

			is List<*> -> ObjectArrayList<Any>().also { out -> value.forEach { out.add(bound(it!!)) } }

			else -> value
		}
		target.parameters.forEach { (name, parameter) -> if (name.length == 1 && (parameter is PatternParameter.Token || parameter is PatternParameter.Predicate)) local[name[0]] = predicate(name) }
		target.predicates.forEach { (symbol, source) ->
			local[symbol] =
				if (source.type == "parameter") {
					predicate(source.properties["parameter"].toString())
				} else {
					val properties = Object2ObjectLinkedOpenHashMap<String, Any>()
					for ((key, value) in source.properties) properties[key] = bound(value)
					PatternPredicateCompiler.compile(source.copy(properties = properties))
				}
		}
		return local
	}

	fun expand(repetitions: IntArray, choices: Map<String, String>): PatternLayout = expand(repetitions, choices, Object2IntMaps.emptyMap())

	fun expand(repetitions: IntArray, choices: Map<String, String>, instanceRepetitions: Object2IntMap<String>): PatternLayout = expand(repetitions, choices, instanceRepetitions, Object2IntMaps.emptyMap())

	fun expand(repetitions: IntArray, choices: Map<String, String>, instanceRepetitions: Object2IntMap<String>, parameterValues: Object2IntMap<String>): PatternLayout {
		var selected = PatternSelections.of(choices)
		instanceRepetitions.object2IntEntrySet().forEach { selected = selected.with("${it.key}#repeat", it.intValue.toString()) }
		return expandResolved(repetitions, selected, parameterValues)
	}

	internal fun expandResolved(repetitions: IntArray, selected: Map<String, String>): PatternLayout = PatternWork { expandResolved(this, repetitions, selected) }.finish()

	internal fun expandResolved(repetitions: IntArray, selected: Map<String, String>, parameterValues: Object2IntMap<String>): PatternLayout = PatternWork { expandResolved(this, repetitions, selected, parameterValues) }.finish()

	internal suspend fun expandResolved(work: PatternWork<*>, repetitions: IntArray, selected: Map<String, String>, parameterValues: Object2IntMap<String> = Object2IntMaps.emptyMap()): PatternLayout {
		val bindings = rootBindings(parameterValues)
		val key = LayoutKey(repetitions, selected, integerParameterNames, integerValues(bindings))
		synchronized(layouts) { layouts.getAndMoveToLast(key)?.let { return it } }
		val root = rootSymbols
		val box = expandNode(work, bind(definition.body, bindings), "$", "$", root, bindings, repetitions, selected, ObjectArrayList(), "", -1, 0, 0)
		definition.sizeConstraints.forEach { constraint ->
			if (!constraint.matches(integerValues(bindings), IntArray(3) { box.size(it) })) box.rejections.add(PatternLayoutRejection(PatternLayoutRejection.Kind.SIZE_CONSTRAINT, constraint.message.ifEmpty { "Pattern size constraint failed" }))
		}
		val origin = definition.origin
		val controllers = ObjectArrayList<PatternLayoutCell>()
		for (cell in box.cells) {
			work.checkpoint()
			if (cell.predicate is PredicateController || (origin.token != "controller" && cell.token == origin.token)) controllers.add(cell)
		}
		val center = if (origin.token == "offset") {
			intArrayOf(origin.offset.depth, origin.offset.height, origin.offset.width)
		} else {
			controllers.first().let { intArrayOf(Math.addExact(it.depth, origin.offset.depth), Math.addExact(it.height, origin.offset.height), Math.addExact(it.width, origin.offset.width)) }
		}
		val cells = ObjectArrayList<PatternLayoutCell>(box.cells.size)
		val layers = Int2ObjectAVLTreeMap<ObjectArrayList<PatternLayoutCell>>()
		box.cells.forEach {
			work.checkpoint()
			val cell = it.copy(depth = Math.subtractExact(it.depth, center[0]), height = Math.subtractExact(it.height, center[1]), width = Math.subtractExact(it.width, center[2]))
			layers.computeIfAbsent(cell.depth) { ObjectArrayList() }.add(cell)
		}
		for (layer in layers.values) {
			for (cell in layer) {
				work.checkpoint()
				cells.add(cell)
			}
		}
		val instances = ObjectArrayList<PatternRepeatInstance>(box.repeats.size)
		box.repeats.forEach { instances.add(it.copy(depth = Math.subtractExact(it.depth, center[0]), height = Math.subtractExact(it.height, center[1]), width = Math.subtractExact(it.width, center[2]))) }
		val modules = transformModules(work, box.modules, PatternPortTransform.IDENTITY, IntArray(3) { Math.negateExact(center[it]) })
		val layout = PatternLayout(ObjectLists.unmodifiable(cells), ObjectLists.unmodifiable(instances), ObjectLists.unmodifiable(box.choices), ObjectLists.unmodifiable(modules), ObjectLists.unmodifiable(box.rejections))
		if (cells.size <= 65536) {
			synchronized(layouts) {
				if (!layouts.containsKey(key)) {
					while (layouts.size >= 32 || cachedCellCount + cells.size > 65536) cachedCellCount -= layouts.removeFirst().cells.size
					layouts[key] = layout
					cachedCellCount += cells.size
				}
			}
		}
		return layout
	}

	private fun fragment(id: String): PatternFragment = definition.fragments[id]
		?: FragmentRegistry.require(ResourceLocation.parse(id))

	private fun slotModules(slot: PatternModuleSlot): ObjectList<PatternModule> = when (slot) {
		is PatternModuleSlot.One -> ObjectLists.singleton(slot.module)
		is PatternModuleSlot.Optional -> ObjectLists.singleton(slot.module)
		is PatternModuleSlot.Chain -> ObjectLists.singleton(slot.module)
		is PatternModuleSlot.OrderedChoice -> ObjectArrayList<PatternModule>(slot.alternatives.size).also { out -> for (alternative in slot.alternatives) out.add(alternative.module) }
	}

	private fun bindSlots(slots: ObjectList<PatternModuleSlot>, values: Map<String, PatternBinding>): ObjectList<PatternModuleSlot> {
		fun module(source: PatternModule): PatternModule {
			val bound = resolveBindings(source.bindings, values)
			val local = PatternBindingContext.overlay(values, bound)
			return source.copy(fragment = if (source.fragment.startsWith("$")) (values[source.fragment.substring(1)] as? PatternBinding.Fragment)?.value ?: error("Missing fragment ${source.fragment}") else source.fragment, bindings = bound, children = bindSlots(source.children, local))
		}
		return ObjectArrayList<PatternModuleSlot>(slots.size).also { out ->
			slots.forEach { slot ->
				out.add(
					when (slot) {
						is PatternModuleSlot.One -> slot.copy(module = module(slot.module))

						is PatternModuleSlot.Optional -> slot.copy(module = module(slot.module))

						is PatternModuleSlot.Chain -> slot.copy(module = module(slot.module))

						is PatternModuleSlot.OrderedChoice -> {
							val alternatives = ObjectArrayList<PatternModuleSlot.OrderedChoice.Alternative>(slot.alternatives.size)
							for (alternative in slot.alternatives) alternatives.add(alternative.copy(module = module(alternative.module)))
							slot.copy(alternatives = alternatives)
						}
					},
				)
			}
		}
	}

	private fun describeSlots(slots: ObjectList<PatternModuleSlot>, host: PatternFragment, prefix: String, stack: ObjectSet<String>, level: Int) {
		for (slot in slots) {
			moduleIds.add(slot.id)
			val modules = slotModules(slot)
			for (index in modules.indices) {
				val module = modules[index]
				val key = when (slot) {
					is PatternModuleSlot.Chain -> "segment"
					is PatternModuleSlot.OrderedChoice -> slot.alternatives[index].id
					else -> "present"
				}
				val target = fragment(module.fragment)
				val path = "$prefix/slot:${slot.id}/module:$key"
				describe(PatternNode.Fragment(module.fragment, module.bindings), path, true, stack, level + 1)
				describeSlots(module.children, target, path, stack, level + 1)
			}
		}
	}

	private fun describe(node: PatternNode, path: String, nested: Boolean, stack: ObjectSet<String>, level: Int) {
		maximumDepth = maxOf(maximumDepth, level)
		when (node) {
			is PatternNode.Assembly -> {
				describe(node.root, "$path/root", nested, stack, level + 1)
				describeSlots(node.slots, fragment(node.root.id), path, stack, level + 1)
			}

			is PatternNode.Fixed -> if (!nested) repeats.add(PatternRepeatDescriptor(repeats.size, path, path, PatternAxis.DEPTH, 1, 1))

			is PatternNode.Sequence -> node.children.forEachIndexed { i, child -> describe(child, "$path/sequence[$i]", nested, stack, level + 1) }

			is PatternNode.Repeat -> {
				val count = node.count
				if (count == null && node.countExpression == null) {
					repeats.add(PatternRepeatDescriptor(repeats.size, node.id, path, PatternAxis.of(node.axis), node.minimum, node.maximum))
				} else if (count != null && definition.measurements.none { it.parameter == count.parameter } && repeats.none { it.parameter == count.parameter }) {
					val parameter = definition.parameters[count.parameter] as PatternParameter.IntegerRange
					repeats.add(PatternRepeatDescriptor(repeats.size, count.parameter, path, PatternAxis.of(node.axis), parameter.minimum, parameter.maximum, count.parameter))
				}
				describe(node.body, "$path/repeat:${node.id}", true, stack, level + 1)
			}

			is PatternNode.Choice -> {
				val ids = ObjectArrayList<String>()
				node.alternatives.forEach {
					ids.add(it.id)
				}
				choices.add(PatternChoiceDescriptor(node.id, path, ObjectLists.unmodifiable(ids), ObjectLists.unmodifiable(ObjectArrayList(node.priority))))
				node.alternatives.forEach { describe(it.node, "$path/choice:${node.id}/${it.id}", true, stack, level + 1) }
			}

			is PatternNode.Fragment -> {
				describe(bind(fragment(node.id).body, node.bindings), "$path/fragment:${node.id}", nested, stack, level + 1)
				stack.remove(node.id)
			}

			is PatternNode.Placed -> describe(node.body, "$path/placed", nested, stack, level + 1)
		}
	}

	private fun compileSymbols(source: Char2ObjectMap<PatternPredicateDefinition>): Char2ObjectMap<PatternPredicate> {
		val result = Char2ObjectLinkedOpenHashMap<PatternPredicate>()
		result[' '] = PatternPredicate.any()
		result['#'] = PatternPredicate.air()
		result['@'] = PatternPredicateCompiler.controller(definition.machine)
		source.forEach { (symbol, value) -> result[symbol] = PatternPredicateCompiler.compile(value) }
		return result
	}

	private fun bind(node: PatternNode, values: Map<String, PatternBinding>): PatternNode = when (node) {
		is PatternNode.Assembly -> {
			val root = bind(node.root, values) as PatternNode.Fragment
			val local = PatternBindingContext.overlay(values, root.bindings)
			node.copy(root = root, slots = bindSlots(node.slots, local))
		}

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
			bindings = resolveBindings(node.bindings, values),
		)

		is PatternNode.Placed -> node.copy(body = bind(node.body, values))
	}

	private data class Box(
		val cells: ObjectArrayList<PatternLayoutCell>,
		val minimum: IntArray,
		val maximum: IntArray,
		val repeats: ObjectArrayList<PatternRepeatInstance> = ObjectArrayList(),
		val choices: ObjectArrayList<PatternChoiceInstance> = ObjectArrayList(),
		val placed: Boolean = false,
		val occupied: Boolean = cells.isNotEmpty(),
		val modules: ObjectArrayList<PatternModuleInstance> = ObjectArrayList(),
		val rejections: ObjectArrayList<PatternLayoutRejection> = ObjectArrayList(),
	) {
		fun size(axis: Int): Int = Math.subtractExact(maximum[axis], minimum[axis])
	}

	private suspend fun expandNode(
		work: PatternWork<*>,
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
		boundsOnly: Boolean = false,
	): Box {
		work.checkpoint()
		val descriptor = if (node is PatternNode.Repeat && node.count != null) repeats.firstOrNull { it.parameter == node.count.parameter } else repeats.firstOrNull { it.path == declaration }
		val currentUnit = if (unit < 0 && descriptor != null) descriptor.index else unit
		return when (node) {
			is PatternNode.Assembly -> {
				val root = expandNode(work, node.root, "$declaration/root", "$path/root", symbols, bindings, repetitions, selected, scopes, repeatPath, currentUnit, repetition, level + 1, boundsOnly)
				val target = fragment(node.root.id)
				val local = localByDeclaration.computeIfAbsent(LocalSymbolKey("$declaration/root", node.root.bindings)) { localSymbols(target, node.root, symbols) }
				val rootBindings = PatternBindingContext.overlay(bindings, node.root.bindings)
				val host = ModuleHost(target, root, PatternPortTransform.IDENTITY, IntArray(3), local, rootBindings)
				val attachments = expandSlots(work, node.slots, host, declaration, path, repetitions, selected, scopes, repeatPath, currentUnit, repetition, level + 1, boundsOnly)
				val merged = overlay(root, attachments)
				if (!boundsOnly) rejectOverlap(merged)
				merged
			}

			is PatternNode.Fixed -> {
				val cells = ObjectArrayList<PatternLayoutCell>()
				if (boundsOnly) return Box(cells, IntArray(3), intArrayOf(node.depth(), node.height(), node.width()), occupied = true)
				node.layers.forEachIndexed { d, layer ->
					layer.forEachIndexed { h, row ->
						row.forEachIndexed { w, symbol ->
							work.checkpoint()
							val predicate = symbols[symbol] ?: throw PatternCompileException("Undeclared local token '$symbol' at $path")
							cells.add(PatternLayoutCell("$path/[$d,$h,$w]", repeatPath, d, h, w, predicate, symbol.toString(), ObjectArrayList(scopes), currentUnit.coerceAtLeast(0), repetition, d, h, w, declaration))
						}
					}
				}
				Box(cells, IntArray(3), intArrayOf(node.depth(), node.height(), node.width()))
			}

			is PatternNode.Sequence -> {
				val children = ObjectArrayList<Box>()
				node.children.forEachIndexed { i, child -> children.add(expandNode(work, child, "$declaration/sequence[$i]", "$path/sequence[$i]", symbols, bindings, repetitions, selected, scopes, repeatPath, currentUnit, repetition, level + 1, boundsOnly)) }
				join(work, children, PatternAxis.of(node.axis).ordinal, false)
			}

			is PatternNode.Repeat -> {
				val declared = descriptor
				val count = node.countExpression?.evaluate(integerValues(bindings)) ?: node.count?.let { count ->
					val value = if (definition.measurements.any { it.parameter == count.parameter }) {
						(bindings[count.parameter] as PatternBinding.IntegerValue).value
					} else {
						repetitions[declared!!.index]
					}
					count.resolve(value)
				} ?: selected["$path#repeat"]?.toInt() ?: repetitions[declared!!.index]
				if (boundsOnly && selected.keys.none { it.startsWith("$path/repeat:${node.id}[") }) {
					if (count == 0) return Box(ObjectArrayList(), IntArray(3), IntArray(3))
					val child = expandNode(work, node.body, "$declaration/repeat:${node.id}", "$path/repeat:${node.id}[0]", symbols, bindings, repetitions, selected, scopes, repeatPath, currentUnit, 0, level + 1, true)
					if (child.placed) return child.copy(placed = false)
					val axis = PatternAxis.of(node.axis).ordinal
					val length = Math.multiplyExact(child.size(axis), count)
					val minimum = IntArray(3) { if (it != axis && !child.occupied) child.minimum[it] else 0 }
					val maximum = IntArray(3) {
						if (it == axis) {
							length
						} else if (child.occupied) {
							child.size(it)
						} else {
							child.maximum[it]
						}
					}
					if (node.direction == PatternRepeatDirection.NEGATIVE) {
						minimum[axis] = -length
						maximum[axis] = 0
					}
					return Box(ObjectArrayList(), minimum, maximum, occupied = child.occupied)
				}
				val children = ObjectArrayList<Box>()
				repeat(count) { i ->
					val instance = "$path/repeat:${node.id}[$i]"
					val childScopes = ObjectArrayList(scopes)
					childScopes.add("node:${node.id}@$instance")
					childScopes.add("repeat:${node.id}@$path")
					children.add(expandNode(work, node.body, "$declaration/repeat:${node.id}", instance, symbols, bindings, repetitions, selected, childScopes, instance, currentUnit, i, level + 1, boundsOnly))
				}
				join(work, children, PatternAxis.of(node.axis).ordinal, node.direction == PatternRepeatDirection.NEGATIVE).also { if (declared != null) it.repeats.add(PatternRepeatInstance(path, declared)) }
			}

			is PatternNode.Choice -> {
				val id = selected[path] ?: selected[declaration] ?: selected[node.id] ?: node.priority.firstOrNull() ?: node.alternatives.first().id
				val branch = node.alternatives.firstOrNull { it.id == id } ?: error("Unknown branch '$id' at $path")
				val childScopes = ObjectArrayList(scopes)
				childScopes.add("node:${node.id}@$path")
				expandNode(work, branch.node, "$declaration/choice:${node.id}/${branch.id}", "$path/choice:${node.id}/${branch.id}", symbols, bindings, repetitions, selected, childScopes, repeatPath, currentUnit, repetition, level + 1, boundsOnly).also {
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
				val reference = node.copy(bindings = resolveBindings(node.bindings, bindings))
				val local = localByDeclaration.computeIfAbsent(LocalSymbolKey(declaration, reference.bindings)) { localSymbols(target, reference, symbols) }
				val localBindings = PatternBindingContext.overlay(bindings, reference.bindings)
				val childScopes = ObjectArrayList(scopes)
				childScopes.add("fragment:$targetId@$path")
				expandNode(work, bind(target.body, localBindings), "$declaration/fragment:${node.id}", "$path/fragment:$targetId", local, localBindings, repetitions, selected, childScopes, repeatPath, currentUnit, repetition, level + 1, boundsOnly)
			}

			is PatternNode.Placed -> {
				val child = expandNode(work, node.body, "$declaration/placed", "$path/placed", symbols, bindings, repetitions, selected, scopes, repeatPath, currentUnit, repetition, level + 1, boundsOnly)
				shift(work, child, intArrayOf(node.offset.depth, node.offset.height, node.offset.width)).copy(placed = true)
			}
		}
	}

	private data class ModuleHost(val fragment: PatternFragment, val shape: Box, val transform: PatternPortTransform, val offset: IntArray, val symbols: Char2ObjectMap<PatternPredicate>, val bindings: Map<String, PatternBinding>)
	private data class ResolvedPort(val point: PatternOrigin.Offset, val direction: PatternDirection, val up: PatternDirection)

	private fun port(host: ModuleHost, id: String): ResolvedPort {
		val port = host.fragment.ports[id] ?: error("Unknown module port '$id'")
		val point = intArrayOf(port.offset.depth, port.offset.height, port.offset.width)
		port.boundary?.let { boundary ->
			val axis = boundary.axis.ordinal
			val boundaryCell = if (boundary.side == PatternPortBoundary.Side.BEFORE) host.shape.minimum[axis] else Math.subtractExact(host.shape.maximum[axis], 1)
			point[axis] = Math.addExact(point[axis], boundaryCell)
		}
		val mapped = host.transform.point(PatternOrigin.Offset(point[0], point[1], point[2]))
		return ResolvedPort(PatternOrigin.Offset(Math.addExact(mapped.depth, host.offset[0]), Math.addExact(mapped.height, host.offset[1]), Math.addExact(mapped.width, host.offset[2])), host.transform.direction(port.direction), host.transform.direction(port.up))
	}

	private suspend fun expandSlots(
		work: PatternWork<*>,
		slots: ObjectList<PatternModuleSlot>,
		host: ModuleHost,
		declaration: String,
		path: String,
		repetitions: IntArray,
		selected: Map<String, String>,
		scopes: ObjectList<String>,
		repeatPath: String,
		unit: Int,
		repetition: Int,
		level: Int,
		boundsOnly: Boolean,
	): Box {
		var result = Box(ObjectArrayList(), IntArray(3), IntArray(3))
		for (slot in slots) {
			work.checkpoint()
			val slotPath = "$path/slot:${slot.id}"
			val slotDeclaration = "$declaration/slot:${slot.id}"
			val explicitValue = selected[slotPath] ?: selected[slotDeclaration] ?: selected[slot.id]
			val candidates = ObjectArrayList<String>()
			var chainCandidates: ObjectList<String>? = null
			val kind: PatternModuleDescriptor.Kind
			var minimum = 1
			var maximum = 1
			var backtrack = true
			when (slot) {
				is PatternModuleSlot.One -> {
					kind = PatternModuleDescriptor.Kind.ONE
					candidates.add("present")
				}

				is PatternModuleSlot.Optional -> {
					kind = PatternModuleDescriptor.Kind.OPTIONAL
					candidates.add("present")
					candidates.add("absent")
					minimum = 0
				}

				is PatternModuleSlot.Chain -> {
					kind = PatternModuleDescriptor.Kind.CHAIN
					minimum = slot.minimum
					maximum = slot.maximum
					backtrack = slot.backtrack
					chainCandidates = PatternModuleDescriptor.ChainCandidates(minimum, maximum)
				}

				is PatternModuleSlot.OrderedChoice -> {
					kind = PatternModuleDescriptor.Kind.ORDERED_CHOICE
					candidates.addAll(slot.priority)
					for (alternative in slot.alternatives) if (!candidates.contains(alternative.id)) candidates.add(alternative.id)
				}
			}
			val orderedCandidates = chainCandidates ?: candidates
			val selection = explicitValue ?: orderedCandidates.first()
			val descriptor = PatternModuleDescriptor(slot.id, slotDeclaration, kind, ObjectLists.unmodifiable(orderedCandidates), explicitValue != null, backtrack, minimum, maximum, slot)
			val module = when (slot) {
				is PatternModuleSlot.One -> slot.module
				is PatternModuleSlot.Optional -> slot.module
				is PatternModuleSlot.Chain -> slot.module
				is PatternModuleSlot.OrderedChoice -> slot.alternatives.first { it.id == selection }.module
			}
			val count = if (slot is PatternModuleSlot.Chain) {
				selection.toInt()
			} else if (selection == "absent") {
				0
			} else {
				1
			}
			val key = if (slot is PatternModuleSlot.Chain) {
				"segment"
			} else if (slot is PatternModuleSlot.OrderedChoice) {
				selection
			} else {
				"present"
			}
			var next = port(host, slot.hostPort)
			var placement = intArrayOf(next.point.depth, next.point.height, next.point.width)
			var attachment = Box(ObjectArrayList(), IntArray(3), IntArray(3))
			val paths = ObjectArrayList<String>()
			val moduleScopes = ObjectArrayList(scopes).also { it.add("node:${slot.id}@$slotPath") }
			for (index in 0 until count) {
				work.checkpoint()
				val segmentPath = "$slotPath/segment[$index]"
				paths.add(segmentPath)
				val moduleDeclaration = "$slotDeclaration/module:$key"
				val reference = PatternNode.Fragment(module.fragment, resolveBindings(module.bindings, host.bindings))
				val target = fragment(reference.id)
				val localBindings = PatternBindingContext.overlay(host.bindings, reference.bindings)
				val localSymbols = localByDeclaration.computeIfAbsent(LocalSymbolKey(moduleDeclaration, reference.bindings)) { localSymbols(target, reference, host.symbols) }
				val localShape = expandNode(work, reference, moduleDeclaration, segmentPath, host.symbols, localBindings, repetitions, selected, moduleScopes, repeatPath, unit, repetition, level + 1, boundsOnly)
				val localHost = ModuleHost(target, localShape, PatternPortTransform.IDENTITY, IntArray(3), localSymbols, localBindings)
				val input = port(localHost, module.input)
				val rotation = PatternPortTransform.between(input.direction, input.up, next.direction, next.up)
				val rotatedInput = rotation.point(input.point)
				val axis = PatternAxis.of(next.direction).ordinal
				val step = if (next.direction.ordinal % 2 == 0) 1 else -1
				val offset = intArrayOf(Math.subtractExact(next.point.depth, rotatedInput.depth), Math.subtractExact(next.point.height, rotatedInput.height), Math.subtractExact(next.point.width, rotatedInput.width))
				offset[axis] = Math.addExact(offset[axis], step)
				if (index == 0) placement = offset
				val childHost = ModuleHost(target, localShape, rotation, offset, localSymbols, localBindings)
				val transformed = transformBox(work, localShape, rotation, offset)
				val nested = expandSlots(work, module.children, childHost, moduleDeclaration, segmentPath, repetitions, selected, moduleScopes, repeatPath, unit, repetition, level + 1, boundsOnly)
				attachment = overlay(attachment, overlay(transformed, nested))
				if (slot is PatternModuleSlot.Chain) next = port(childHost, module.output)
			}
			val instance = PatternModuleInstance(slotPath, descriptor, selection, ObjectLists.unmodifiable(ObjectArrayList(attachment.modules)), module.fragment, placement[0], placement[1], placement[2], ObjectLists.unmodifiable(paths))
			attachment = attachment.copy(modules = ObjectArrayList<PatternModuleInstance>().also { it.add(instance) })
			result = overlay(result, attachment)
		}
		return result
	}

	private fun overlay(first: Box, second: Box): Box {
		val occupied = first.occupied || second.occupied
		val minimum = IntArray(3) { axis ->
			if (!first.occupied) {
				second.minimum[axis]
			} else if (!second.occupied) {
				first.minimum[axis]
			} else {
				minOf(first.minimum[axis], second.minimum[axis])
			}
		}
		val maximum = IntArray(3) { axis ->
			if (!first.occupied) {
				second.maximum[axis]
			} else if (!second.occupied) {
				first.maximum[axis]
			} else {
				maxOf(first.maximum[axis], second.maximum[axis])
			}
		}
		first.cells.addAll(second.cells)
		first.repeats.addAll(second.repeats)
		first.choices.addAll(second.choices)
		first.modules.addAll(second.modules)
		first.rejections.addAll(second.rejections)
		return first.copy(minimum = minimum, maximum = maximum, occupied = occupied)
	}

	private fun rejectOverlap(box: Box) {
		val occupied = ObjectOpenHashSet<PatternOrigin.Offset>()
		for ((_, _, depth, height, width, predicate) in box.cells) {
			if (!predicate.isAny() && !occupied.add(
					PatternOrigin.Offset(
						depth,
						height,
						width,
					),
				)
			) {
				box.rejections.add(PatternLayoutRejection(PatternLayoutRejection.Kind.MODULE_OVERLAP, "Constrained module cells overlap at ($depth,$height,$width)"))
			}
		}
	}

	private suspend fun transformModules(work: PatternWork<*>, instances: ObjectList<PatternModuleInstance>, transform: PatternPortTransform, offset: IntArray): ObjectArrayList<PatternModuleInstance> {
		val result = ObjectArrayList<PatternModuleInstance>(instances.size)
		for (instance in instances) {
			work.checkpoint()
			val point = transform.point(PatternOrigin.Offset(instance.depth, instance.height, instance.width))
			result.add(instance.copy(depth = Math.addExact(point.depth, offset[0]), height = Math.addExact(point.height, offset[1]), width = Math.addExact(point.width, offset[2]), children = ObjectLists.unmodifiable(transformModules(work, instance.children, transform, offset))))
		}
		return result
	}

	private suspend fun transformBox(work: PatternWork<*>, box: Box, transform: PatternPortTransform, offset: IntArray): Box {
		val cells = ObjectArrayList<PatternLayoutCell>(box.cells.size)
		for (cell in box.cells) {
			work.checkpoint()
			val point = transform.point(PatternOrigin.Offset(cell.depth, cell.height, cell.width))
			cells.add(cell.copy(depth = Math.addExact(point.depth, offset[0]), height = Math.addExact(point.height, offset[1]), width = Math.addExact(point.width, offset[2])))
		}
		val minimum = IntArray(3) { Int.MAX_VALUE }
		val maximum = IntArray(3) { Int.MIN_VALUE }
		if (box.occupied) {
			for (corner in 0..7) {
				val source = IntArray(3) { axis -> if (corner and (1 shl axis) == 0) box.minimum[axis] else Math.subtractExact(box.maximum[axis], 1) }
				val point = transform.point(PatternOrigin.Offset(source[0], source[1], source[2]))
				val target = intArrayOf(Math.addExact(point.depth, offset[0]), Math.addExact(point.height, offset[1]), Math.addExact(point.width, offset[2]))
				for (axis in 0..2) {
					minimum[axis] = minOf(minimum[axis], target[axis])
					maximum[axis] = maxOf(maximum[axis], Math.addExact(target[axis], 1))
				}
			}
		}
		val repeatInstances = ObjectArrayList<PatternRepeatInstance>()
		for (instance in box.repeats) {
			val point = transform.point(PatternOrigin.Offset(instance.depth, instance.height, instance.width))
			val direction = when (instance.descriptor.axis) {
				PatternAxis.DEPTH -> PatternDirection.FRONT
				PatternAxis.HEIGHT -> PatternDirection.UP
				PatternAxis.WIDTH -> PatternDirection.LEFT
			}
			repeatInstances.add(instance.copy(descriptor = instance.descriptor.copy(axis = PatternAxis.of(transform.direction(direction))), depth = Math.addExact(point.depth, offset[0]), height = Math.addExact(point.height, offset[1]), width = Math.addExact(point.width, offset[2])))
		}
		return Box(cells, if (box.occupied) minimum else IntArray(3), if (box.occupied) maximum else IntArray(3), repeatInstances, ObjectArrayList(box.choices), box.placed, box.occupied, transformModules(work, box.modules, transform, offset), ObjectArrayList(box.rejections))
	}

	private suspend fun join(work: PatternWork<*>, children: ObjectList<Box>, axis: Int, negative: Boolean): Box {
		val crossSize = IntArray(3)
		children.forEach { box -> if (!box.placed) for (i in 0..2) crossSize[i] = maxOf(crossSize[i], box.size(i)) }
		val cells = ObjectArrayList<PatternLayoutCell>()
		val repeatInstances = ObjectArrayList<PatternRepeatInstance>()
		val choiceInstances = ObjectArrayList<PatternChoiceInstance>()
		val moduleInstances = ObjectArrayList<PatternModuleInstance>()
		val rejections = ObjectArrayList<PatternLayoutRejection>()
		val minimum = IntArray(3) { Int.MAX_VALUE }
		val maximum = IntArray(3) { Int.MIN_VALUE }
		var cursor = 0
		children.forEach { box ->
			val offset = IntArray(3) { i ->
				if (box.placed) {
					0
				} else if (i == axis) {
					if (negative) Math.subtractExact(-cursor, box.maximum[i]) else Math.subtractExact(cursor, box.minimum[i])
				} else if (!box.occupied) {
					0
				} else {
					if ((crossSize[i] - box.size(i)) % 2 != 0) rejections.add(PatternLayoutRejection(PatternLayoutRejection.Kind.SIZE_CONSTRAINT, "Cannot center nodes of incompatible parity; place the node with an explicit offset"))
					Math.subtractExact((crossSize[i] - box.size(i)) / 2, box.minimum[i])
				}
			}
			val shifted = shift(work, box, offset)
			for (cell in shifted.cells) {
				work.checkpoint()
				cells.add(cell)
			}
			for (instance in shifted.repeats) {
				work.checkpoint()
				repeatInstances.add(instance)
			}
			for (instance in shifted.choices) {
				work.checkpoint()
				choiceInstances.add(instance)
			}
			moduleInstances.addAll(shifted.modules)
			rejections.addAll(shifted.rejections)
			for (i in 0..2) {
				minimum[i] = minOf(minimum[i], shifted.minimum[i])
				maximum[i] = maxOf(maximum[i], shifted.maximum[i])
			}
			if (!box.placed) cursor = Math.addExact(cursor, box.size(axis))
		}
		return Box(cells, if (children.isEmpty()) IntArray(3) else minimum, if (children.isEmpty()) IntArray(3) else maximum, repeatInstances, choiceInstances, occupied = children.any { it.occupied }, modules = moduleInstances, rejections = rejections)
	}

	private suspend fun shift(work: PatternWork<*>, box: Box, offset: IntArray): Box {
		val cells = ObjectArrayList<PatternLayoutCell>(box.cells.size)
		box.cells.forEach {
			work.checkpoint()
			cells.add(it.copy(depth = Math.addExact(it.depth, offset[0]), height = Math.addExact(it.height, offset[1]), width = Math.addExact(it.width, offset[2])))
		}
		val repeats = ObjectArrayList<PatternRepeatInstance>(box.repeats.size)
		for (repeat in box.repeats) {
			work.checkpoint()
			repeats.add(repeat.copy(depth = Math.addExact(repeat.depth, offset[0]), height = Math.addExact(repeat.height, offset[1]), width = Math.addExact(repeat.width, offset[2])))
		}
		val modules = transformModules(work, box.modules, PatternPortTransform.IDENTITY, offset)
		return Box(cells, IntArray(3) { Math.addExact(box.minimum[it], offset[it]) }, IntArray(3) { Math.addExact(box.maximum[it], offset[it]) }, repeats, box.choices, box.placed, box.occupied, modules, ObjectArrayList(box.rejections))
	}
}
