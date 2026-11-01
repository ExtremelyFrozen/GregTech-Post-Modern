package com.gregtechceu.gtceu.api.multiblock.pattern.predicate

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.GTCEuAPI
import com.gregtechceu.gtceu.api.block.MetaMachineBlock
import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gregtechceu.gtceu.api.machine.feature.ITieredMachine
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility
import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo
import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternPredicateDefinition
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.FramePredicate
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.StructurePredicate
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection
import com.gregtechceu.gtceu.api.pipenet.IPipeNode

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.Property

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.mojang.serialization.JsonOps
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectList
import it.unimi.dsi.fastutil.objects.ObjectLists
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import it.unimi.dsi.fastutil.objects.ObjectSets

import java.util.function.Function
import java.util.function.Supplier

/** Built-in semantic predicates and the compatibility boundary for existing predicate codecs. */
object PatternPredicateCompiler {
	private val gson = Gson()

	@JvmStatic
	fun controller(machine: ResourceLocation): PatternPredicate = PredicateController(
		PatternPredicate(
			{ state -> MetaMachine.getMachine(state.world, state.pos)?.definition?.id == machine },
			{ if (BuiltInRegistries.BLOCK.containsKey(machine)) MultiblockBlockInfo.fromBlock(BuiltInRegistries.BLOCK.get(machine)) else MultiblockBlockInfo.EMPTY },
			{ if (BuiltInRegistries.BLOCK.containsKey(machine)) arrayOf(BuiltInRegistries.BLOCK.get(machine)) else emptyArray() },
		),
	)

	@JvmStatic
	fun compile(definition: PatternPredicateDefinition): PatternPredicate {
		(definition.properties["runtime"] as? PatternPredicate)?.let { return it }
		val typeId = if (definition.type.contains(':')) ResourceLocation.parse(definition.type) else GTCEu.id(definition.type)
		PredicateRegistry.find(typeId)?.let { return it.compile(definition) }
		if (typeId.namespace != GTCEu.MOD_ID && typeId.namespace != "gtceu") return compileRegisteredCodec(definition)
		val type = definition.type.substringAfter(':')
		val properties = definition.properties
		val candidates = ObjectArrayList<Block>()
		var candidateSupplier: Supplier<Array<Block>>? = null
		var previewStates: Supplier<ObjectList<BlockState>>? = null
		val observer: (MultiblockState) -> PatternObservation
		val candidateObserver: (BlockState) -> PatternObservation
		fun result(matched: Boolean, facts: Collection<String> = emptyList(), attributes: Map<String, Any> = emptyMap()): PatternObservation =
			PatternObservation(matched, if (matched) ObjectSets.unmodifiable(ObjectOpenHashSet(facts).also { it.addAll(definition.facts) }) else ObjectSets.emptySet(), if (matched) Object2ObjectMaps.unmodifiable(Object2ObjectLinkedOpenHashMap(attributes)) else Object2ObjectMaps.emptyMap())
		when (type) {
			"any" -> return ObservedPatternPredicate({ result(true) }, { emptyArray() }, "any", { result(true) })

			"air" -> return ObservedPatternPredicate(
				{ state -> result(state.blockState.isAir) },
				{ arrayOf(Blocks.AIR) },
				"air",
				{ result(it.isAir) },
			)

			"controller" -> return controller(ResourceLocation.parse(properties["machine"]?.toString() ?: error("Controller predicate requires machine")))

			"block" -> {
				val ids = properties["ids"] as? List<*> ?: properties["blocks"] as? List<*> ?: error("Block predicate requires ids")
				require(ids.isNotEmpty()) { "Block ids must not be empty" }
				ids.forEach { candidates.add(requireBlock(it.toString())) }
				candidateObserver = { state -> result(candidates.contains(state.block)) }
				observer = { state -> candidateObserver(state.blockState) }
			}

			"block_state" -> {
				val block = requireBlock(properties["block"]?.toString() ?: error("Missing block"))
				candidates.add(block)
				val expected = Object2ObjectLinkedOpenHashMap<Property<*>, String>()
				var preview = block.defaultBlockState()
				(properties["properties"] as? Map<*, *>)?.forEach { (name, value) ->
					val property = block.stateDefinition.getProperty(name.toString()) ?: error("Unknown property $name")
					require(property.possibleValues.any { it.toString().equals(value.toString(), true) }) { "Unknown value $value for $name" }
					expected[property] = value.toString()
					preview = applyProperty(preview, property, value.toString())
				}
				previewStates = Supplier { ObjectLists.singleton(preview) }
				candidateObserver = { state -> result(state.block == block && expected.all { (property, value) -> state.getValue(property).toString().equals(value, true) }) }
				observer = { state -> candidateObserver(state.blockState) }
			}

			"block_tag" -> {
				val tag = TagKey.create(BuiltInRegistries.BLOCK.key(), ResourceLocation.parse(properties["tag"].toString()))
				BuiltInRegistries.BLOCK.getTag(tag).ifPresent { holders -> holders.forEach { candidates.add(it.value()) } }
				candidateSupplier = Supplier { ObjectArrayList<Block>().also { out -> BuiltInRegistries.BLOCK.getTag(tag).ifPresent { holders -> holders.forEach { out.add(it.value()) } } }.toTypedArray() }
				candidateObserver = { state -> result(state.`is`(tag)) }
				observer = { state -> candidateObserver(state.blockState) }
			}

			"part" -> {
				val names = properties["abilities"] as? List<*> ?: error("Part predicate requires abilities")
				val abilities = ObjectArrayList<PartAbility>()
				names.forEach { abilities.add(PartAbility.byName(it.toString()).orElseThrow { IllegalArgumentException("Unknown ability $it") }) }
				require(abilities.isNotEmpty()) { "Part abilities must not be empty" }
				val tiers = properties["tiers"] as? Map<*, *>
				val min = (tiers?.get("min") as? Number)?.toInt() ?: Int.MIN_VALUE
				val max = (tiers?.get("max") as? Number)?.toInt() ?: Int.MAX_VALUE
				require(min <= max) { "Invalid part tier range" }
				abilities.forEach { candidates.addAll(it.getBlockRange(min, max)) }
				val allowed = properties["allowedTiers"] as? IntArray
				val io = properties["io"]?.toString()
				require(io == null || io == "input" || io == "output" || io == "both") { "Unknown part IO '$io'" }
				fun observePart(state: BlockState, tier: Int?, isPart: Boolean): PatternObservation {
					val actual = ObjectArrayList<String>()
					if (isPart && tier != null && tier in min..max && (allowed == null || allowed.contains(tier))) abilities.forEach { if (it.isApplicable(state.block)) actual.add("part.${it.name}") }
					val matched = actual.isNotEmpty() && (io == null || io == "both" || actual.any { if (io == "input") it.contains("input") || it.contains("import") else it.contains("output") || it.contains("export") })
					if (matched) actual.add("part")
					return result(matched, actual)
				}
				candidateObserver = { state ->
					val block = state.block as? MetaMachineBlock
					observePart(state, block?.definition?.tier, block != null)
				}
				observer = { state ->
					val machine = MetaMachine.getMachine(state.world, state.pos)
					val facing = properties["facing"]?.toString()?.let { name -> state.controller?.let { controller -> RelativeDirection.valueOf(name.uppercase()).getRelative(controller.frontFacing, controller.upwardsFacing, state.isNeededFlip) } }
					if (properties.containsKey("facing") && (facing == null || machine?.frontFacing != facing)) result(false) else observePart(state.blockState, (machine as? ITieredMachine)?.tier ?: machine?.definition?.tier, machine is IMultiPart)
				}
			}

			"coil", "heating_coils" -> {
				GTCEuAPI.HEATING_COILS.values.forEach { candidates.add(it.get()) }
				candidateObserver = { state ->
					val stats = GTCEuAPI.HEATING_COILS.entries.firstOrNull { it.value.get() == state.block }?.key
					result(stats != null, listOf("coil"), if (stats != null) mapOf("CoilType" to stats, "coil.temperature" to stats.coilTemperature, "coil.tier" to stats.tier) else emptyMap())
				}
				observer = { state -> candidateObserver(state.blockState) }
			}

			"filter", "cleanroom_filters" -> {
				GTCEuAPI.CLEANROOM_FILTERS.values.forEach { candidates.add(it.get()) }
				candidateObserver = { state ->
					val stats = GTCEuAPI.CLEANROOM_FILTERS.entries.firstOrNull { it.value.get() == state.block }?.key
					result(stats != null, ObjectLists.singleton("filter"), if (stats == null) Object2ObjectMaps.emptyMap() else Object2ObjectMaps.singleton("FilterType", stats))
				}
				observer = { state -> candidateObserver(state.blockState) }
			}

			"frame", "frames" -> {
				val values = properties["materials"] ?: error("Frame predicate requires materials")
				val ids = ObjectArrayList<ResourceLocation>()
				(values as? List<*> ?: ObjectLists.singleton(values)).forEach { value ->
					val id = value.toString()
					ids.add(if (id.contains(':')) ResourceLocation.parse(id) else GTCEu.id(id))
				}
				val frame = FramePredicate(ids)
				candidates.addAll(frame.frameBlocks())
				candidateObserver = { state -> result(frame.frameBlocks().contains(state.block), ObjectLists.singleton("frame")) }
				observer = { state ->
					val block = candidateObserver(state.blockState)
					val pipe = state.blockEntity as? IPipeNode<*, *>
					if (block.matched) block else result(pipe != null && frame.materials().contains(pipe.frameMaterial), ObjectLists.singleton("frame"))
				}
			}

			"all_of", "any_of", "one_of", "not" -> {
				val children = ObjectArrayList<Pair<String, PatternPredicate>>()
				when (type) {
					"all_of" -> {
						(properties["items"] as? List<*>)?.forEachIndexed { i, child ->
							children.add(
								i.toString() to compile(
									nested(
										child,
									),
								),
							)
						} ?: error("Missing all_of items")
					}

					"not" -> children.add("not" to compile(nested(properties["item"])))

					else -> {
						(properties["alternatives"] as? List<*>)?.forEach { child ->
							val alternative = child as? Map<*, *> ?: error("Expected alternative")
							val childDefinition = nested(alternative["predicate"])
							val facts = ObjectArrayList(childDefinition.facts)
							(alternative["facts"] as? List<*>)?.forEach { facts.add(it.toString()) }
							children.add(alternative["id"].toString() to compile(childDefinition.copy(facts = facts)))
						} ?: error("Missing alternatives")
					}
				}
				require(children.isNotEmpty()) { "Composite predicate requires children" }
				val priority = properties["priority"] as? List<*>
				val exclusive = type == "one_of" || properties["selection"] == "exclusive"
				require(type != "any_of" || exclusive || priority != null) { "any_of requires exclusive selection or stable priority" }
				if (priority != null) require(priority.size == children.size && ObjectOpenHashSet(priority) == ObjectOpenHashSet(children.map { it.first })) { "Priority must name every predicate alternative exactly once" }
				fun combined(visit: (PatternPredicate) -> PatternObservation): PatternObservation {
					val success = ObjectArrayList<Pair<String, PatternObservation>>()
					children.forEach { (id, child) ->
						val observation = visit(child)
						if (observation.matched) success.add(id to observation)
					}
					val valid = when (type) {
						"all_of" -> success.size == children.size
						"not" -> success.isEmpty
						else -> if (exclusive) success.size == 1 else success.isNotEmpty()
					}
					val facts = ObjectOpenHashSet<String>()
					val attributes = Object2ObjectLinkedOpenHashMap<String, Any>()
					if (valid && type != "not") {
						val chosen = if (type == "all_of") {
							success
						} else {
							listOf(if (priority == null) success.first() else success.minBy { priority.indexOf(it.first).let { i -> if (i < 0) Int.MAX_VALUE else i } })
						}
						for ((_, item) in chosen) {
							facts.addAll(item.facts)
							for ((key, value) in item.properties) {
								val previous = attributes.putIfAbsent(key, value)
								if (previous != null && previous != value) return result(false)
							}
						}
					}
					return result(valid, facts, attributes)
				}
				candidateObserver = { state -> combined { it.observeCandidate(state) } }
				observer = { state -> combined { observe(it, state) } }
				previewStates = Supplier {
					val states = ObjectOpenHashSet<BlockState>()
					children.forEach { (_, child) -> child.getPreviewCandidates().forEach { states.add(it.blockInfo.blockState) } }
					if (type == "not") BuiltInRegistries.BLOCK.firstOrNull { candidateObserver(it.defaultBlockState()).matched }?.let { states.add(it.defaultBlockState()) }
					ObjectArrayList(states)
				}
				candidateSupplier = Supplier {
					val states = previewStates!!.get()
					Array(states.size) { states[it].block }
				}
			}

			else -> {
				return compileRegisteredCodec(definition)
			}
		}
		return ObservedPatternPredicate(Function(observer), candidateSupplier ?: Supplier { candidates.toTypedArray() }, candidateObservation = Function(candidateObserver), states = previewStates).also { predicate ->
			if (type == "part") properties["facing"]?.let { predicate.setRelativeDirection(RelativeDirection.valueOf(it.toString().uppercase())) }
		}
	}

	private fun compileRegisteredCodec(definition: PatternPredicateDefinition): PatternPredicate {
		val json = JsonObject().apply {
			addProperty("type", definition.type)
			definition.properties.forEach { (key, value) -> add(key, gson.toJsonTree(value)) }
		}
		return PatternPredicate(StructurePredicate.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow { error -> IllegalArgumentException(error) }).withFacts(definition.facts)
	}

	private fun <T : Comparable<T>> applyProperty(state: BlockState, property: Property<T>, value: String): BlockState = state.setValue(property, property.getValue(value).orElseThrow { IllegalArgumentException("Unknown value '$value' for ${property.name}") })

	fun observe(predicate: PatternPredicate, state: MultiblockState): PatternObservation = if (predicate is ObservedPatternPredicate) predicate.observe(state) else PatternObservation(predicate.test(state), ObjectOpenHashSet(predicate.facts), Object2ObjectMaps.emptyMap())

	private fun nested(value: Any?): PatternPredicateDefinition {
		val map = value as? Map<*, *> ?: error("Expected nested predicate")
		val properties = Object2ObjectLinkedOpenHashMap<String, Any>()
		map.forEach { (key, item) -> if (key != "type" && key != "facts" && key != "name" && item != null) properties[key.toString()] = item }
		val facts = ObjectArrayList<String>()
		(map["facts"] as? List<*>)?.forEach { facts.add(it.toString()) }
		return PatternPredicateDefinition(map["type"]?.toString() ?: error("Missing predicate type"), map["name"]?.toString() ?: "", properties, facts)
	}

	private fun requireBlock(id: String): Block {
		val location = ResourceLocation.parse(id)
		require(BuiltInRegistries.BLOCK.containsKey(location)) { "Unknown block $id" }
		return BuiltInRegistries.BLOCK.get(location)
	}
}
