package com.gregtechceu.gtceu.api.multiblock.pattern.dsl

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition
import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.api.multiblock.PatternCondition
import com.gregtechceu.gtceu.api.multiblock.StructureDir
import com.gregtechceu.gtceu.api.multiblock.pattern.match.MultiBlockPattern
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternAxes
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternBinding
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternConstraint
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternDefinition
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternDirection
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternFragment
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternNode
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternOrientation
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternOrigin
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternParameter
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternPredicateDefinition
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternRepeatDirection
import com.gregtechceu.gtceu.api.multiblock.pattern.model.expression.PatternSizeConstraint
import com.gregtechceu.gtceu.api.multiblock.pattern.model.measurement.PatternMeasurement
import com.gregtechceu.gtceu.api.multiblock.pattern.model.module.PatternModuleSlot
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.ObservedPatternPredicate
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.PatternPredicate
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.PredicateController
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection
import com.gregtechceu.gtceu.data.pattern.StructurePatternKey
import com.gregtechceu.gtceu.data.pattern.StructurePatternResolver

import it.unimi.dsi.fastutil.chars.Char2ObjectArrayMap
import it.unimi.dsi.fastutil.chars.Char2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.chars.Char2ObjectMap
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectList
import it.unimi.dsi.fastutil.objects.ObjectLists

import java.util.function.Predicate

/** Java DSL entry point for defining a multiblock structure. */
class PatternBuilder private constructor(charDir: RelativeDirection, stringDir: RelativeDirection, aisleDir: RelativeDirection, private val definition: MultiblockMachineDefinition, private val structureName: String) {
	private val definitionKey get() = StructurePatternKey(definition.id, structureName)
	private val units = ObjectArrayList<AisleUnit>()
	private val symbolMap: Char2ObjectMap<PatternPredicate?> = Char2ObjectArrayMap()
	private var structureDir = StructureDir(charDir, stringDir, aisleDir)
	private var repeatableGroup: ObjectArrayList<Array<String>>? = null
	private var condition: PatternCondition? = null
	private var aisleHeight = 0
	private var rowWidth = 0
	private val extraNodes = ObjectArrayList<PatternNode>()
	private val constraints = ObjectArrayList<PatternConstraint>()
	private val fragments = Object2ObjectLinkedOpenHashMap<String, PatternFragment>()
	private var orientation = PatternOrientation(true, true)
	private var origin = PatternOrigin.controller()
	private val parameters = Object2ObjectLinkedOpenHashMap<String, PatternParameter>()
	private val measurements = ObjectArrayList<PatternMeasurement>()
	private val sizeConstraints = ObjectArrayList<PatternSizeConstraint>()

	fun measurement(measurement: PatternMeasurement): PatternBuilder = apply { measurements.add(measurement) }
	fun sizeConstraint(constraint: PatternSizeConstraint): PatternBuilder = apply { sizeConstraints.add(constraint) }
	fun assembly(root: PatternNode.Fragment, slots: List<PatternModuleSlot>): PatternBuilder = node(PatternNode.Assembly(root, slots))

	fun axes(axes: PatternAxes): PatternBuilder {
		structureDir = StructureDir(RelativeDirection.valueOf(axes.width.name), RelativeDirection.valueOf(axes.height.name), RelativeDirection.valueOf(axes.depth.name))
		return this
	}

	fun origin(origin: PatternOrigin): PatternBuilder {
		this.origin = origin
		return this
	}

	fun parameter(name: String, parameter: PatternParameter): PatternBuilder {
		parameters[name] = parameter
		return this
	}

	fun orientation(orientation: PatternOrientation): PatternBuilder {
		this.orientation = orientation
		return this
	}

	fun fragment(id: String, fragment: PatternFragment): PatternBuilder {
		fragments[id] = fragment
		return this
	}

	fun node(node: PatternNode): PatternBuilder {
		extraNodes.add(node)
		return this
	}

	private data class AisleUnit(val slices: ObjectList<Array<String>>, val minRepeat: Int, val maxRepeat: Int)

	init {
		symbolMap[' '] = PatternPredicate.any()
		symbolMap['~'] = PatternPredicate.controller(PatternPredicate.blocks(definition.block))
	}

	/** Adds a single aisle to this pattern. Multiple calls increase the aisleDir by 1. */
	fun aisle(vararg aisle: String): PatternBuilder {
		validateAisle(aisle)
		val group = repeatableGroup
		if (group == null) {
			units.add(AisleUnit(ObjectLists.singleton(copyAisle(aisle)), 1, 1))
		} else {
			group.add(copyAisle(aisle))
		}
		return this
	}

	fun slice(vararg rows: String): PatternBuilder = aisle(*rows)

	fun sliceRepeatable(min: Int, max: Int, vararg rows: String): PatternBuilder = beginRepeatable().aisle(*rows).endRepeatable(min, max)

	fun use(fragmentId: String, bindings: Map<String, PatternBinding>): PatternBuilder {
		extraNodes.add(PatternNode.Fragment(fragmentId, bindings))
		return this
	}

	fun place(fragmentId: String, anchor: PatternOrigin.Offset): PatternBuilder {
		extraNodes.add(PatternNode.Placed(PatternNode.Fragment(fragmentId, Object2ObjectMaps.emptyMap()), anchor))
		return this
	}

	fun constraint(constraint: PatternConstraint): PatternBuilder {
		constraints.add(constraint)
		return this
	}

	fun appendDefinition(): PatternBuilder = appendDefinition(definitionKey)

	fun appendDefinition(key: StructurePatternKey): PatternBuilder = StructurePatternResolver.appendDefinition(this, key)

	fun beginRepeatable(): PatternBuilder {
		repeatableGroup = ObjectArrayList()
		return this
	}

	fun endRepeatable(minRepeat: Int, maxRepeat: Int): PatternBuilder {
		val group = repeatableGroup!!
		units.add(AisleUnit(ObjectArrayList(group), minRepeat, maxRepeat))
		repeatableGroup = null
		return this
	}

	fun endRepeatable(repeatCount: Int): PatternBuilder = endRepeatable(repeatCount, repeatCount)

	private fun validateAisle(aisle: Array<out String>) {
		if (units.isEmpty() && (repeatableGroup == null || repeatableGroup!!.isEmpty())) {
			aisleHeight = aisle.size
			rowWidth = aisle[0].length
		}
		aisle.forEach { row ->
			row.forEach { symbol ->
				if (!symbolMap.containsKey(symbol)) symbolMap[symbol] = null
			}
		}
	}

	private fun copyAisle(aisle: Array<out String>): Array<String> = Array(aisle.size) { index -> aisle[index] }

	fun where(symbol: String, blockMatcher: PatternPredicate): PatternBuilder = where(symbol[0], blockMatcher)

	fun where(symbol: Char, blockMatcher: PatternPredicate): PatternBuilder {
		symbolMap[symbol] = when {
			blockMatcher.isAny() || blockMatcher.isAir() -> blockMatcher
			blockMatcher is PredicateController -> blockMatcher.sort()
			blockMatcher is ObservedPatternPredicate -> blockMatcher
			else -> PatternPredicate(blockMatcher).sort()
		}
		return this
	}

	fun condition(condition: Predicate<MultiblockState>): PatternBuilder = condition(condition, "gtceu.recipe_logic.condition_fails")

	fun condition(condition: Predicate<MultiblockState>, translateKey: String): PatternBuilder {
		this.condition = PatternCondition(condition, translateKey)
		return this
	}

	fun build(): MultiBlockPattern = MultiBlockPattern.fromDefinition(buildDefinition()).also { it.condition = condition }

	/** Builds the canonical AST consumed by JSON, binary and compiler paths. */
	fun buildDefinition(): PatternDefinition {
		val nodes = ObjectArrayList<PatternNode>(units.size + extraNodes.size)
		units.forEachIndexed { index, unit ->
			val layers = ObjectArrayList<ObjectList<String>>(unit.slices.size)
			unit.slices.forEach { slice -> layers.add(ObjectArrayList(slice)) }
			val fixed = PatternNode.Fixed(layers)
			nodes.add(
				if (unit.minRepeat == 1 && unit.maxRepeat == 1) {
					fixed
				} else {
					PatternNode.Repeat(
						"unit-$index",
						PatternDirection.FRONT,
						PatternRepeatDirection.POSITIVE,
						unit.minRepeat,
						unit.maxRepeat,
						fixed,
					)
				},
			)
		}
		nodes.addAll(extraNodes)
		val body = if (nodes.size == 1) nodes.first() else PatternNode.Sequence(PatternDirection.FRONT, nodes)
		val predicates = Char2ObjectLinkedOpenHashMap<PatternPredicateDefinition>()
		symbolMap.char2ObjectEntrySet().forEach { entry ->
			predicates[entry.charKey] = PatternPredicateDefinition("java", "", Object2ObjectMaps.singleton<String, Any>("runtime", entry.value!!), ObjectLists.emptyList())
		}
		return PatternDefinition(
			definition.id,
			definitionKey.resourceId(),
			PatternAxes(
				toPatternDirection(structureDir.aisleDir()),
				toPatternDirection(structureDir.stringDir()),
				toPatternDirection(structureDir.charDir()),
			),
			orientation,
			origin,
			parameters,
			fragments,
			predicates,
			body,
			constraints,
			measurements,
			sizeConstraints,
		)
	}

	companion object {
		private val DEFAULT_STRUCTURE = StructurePatternKey.DEFAULT_STRUCTURE_NAME

		@JvmStatic
		fun start(definition: MultiblockMachineDefinition): PatternBuilder = PatternBuilder(
			RelativeDirection.LEFT,
			RelativeDirection.UP,
			RelativeDirection.FRONT,
			definition,
			DEFAULT_STRUCTURE,
		)

		@JvmStatic
		fun start(definition: MultiblockMachineDefinition, structureName: String): PatternBuilder = PatternBuilder(
			RelativeDirection.LEFT,
			RelativeDirection.UP,
			RelativeDirection.FRONT,
			definition,
			structureName,
		)

		@JvmStatic
		fun start(definition: MultiblockMachineDefinition, charDir: RelativeDirection, stringDir: RelativeDirection, aisleDir: RelativeDirection): PatternBuilder = PatternBuilder(charDir, stringDir, aisleDir, definition, DEFAULT_STRUCTURE)

		@JvmStatic
		fun start(definition: MultiblockMachineDefinition, structureName: String, charDir: RelativeDirection, stringDir: RelativeDirection, aisleDir: RelativeDirection): PatternBuilder = PatternBuilder(charDir, stringDir, aisleDir, definition, structureName)

		private fun toPatternDirection(direction: RelativeDirection): PatternDirection = PatternDirection.valueOf(direction.name)
	}
}
