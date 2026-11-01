package com.gregtechceu.gtceu.api.multiblock.pattern.match

import com.gregtechceu.gtceu.api.block.ActiveBlock
import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart
import com.gregtechceu.gtceu.api.multiblock.CenterOffset
import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.api.multiblock.PatternCondition
import com.gregtechceu.gtceu.api.multiblock.RelativeOffset
import com.gregtechceu.gtceu.api.multiblock.StructureDir
import com.gregtechceu.gtceu.api.multiblock.error.PatternError
import com.gregtechceu.gtceu.api.multiblock.error.PatternStringError
import com.gregtechceu.gtceu.api.multiblock.error.SinglePredicateError
import com.gregtechceu.gtceu.api.multiblock.pattern.compile.CompiledPattern
import com.gregtechceu.gtceu.api.multiblock.pattern.compile.PatternCompiler
import com.gregtechceu.gtceu.api.multiblock.pattern.layout.PatternAxis
import com.gregtechceu.gtceu.api.multiblock.pattern.layout.PatternChoiceDescriptor
import com.gregtechceu.gtceu.api.multiblock.pattern.layout.PatternLayout
import com.gregtechceu.gtceu.api.multiblock.pattern.layout.PatternLayoutCell
import com.gregtechceu.gtceu.api.multiblock.pattern.layout.PatternLayoutResolver
import com.gregtechceu.gtceu.api.multiblock.pattern.layout.PatternRepeatDescriptor
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternDefinition
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternFacts
import com.gregtechceu.gtceu.api.multiblock.pattern.predicate.ObservedPatternPredicate
import com.gregtechceu.gtceu.api.multiblock.pattern.predicate.PatternPredicate
import com.gregtechceu.gtceu.api.multiblock.pattern.predicate.PatternPredicates
import com.gregtechceu.gtceu.api.multiblock.pattern.predicate.PredicateRule
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.RestrictedPredicate
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.StructurePredicate
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.DirectionProperty
import net.minecraft.world.level.block.state.properties.Property

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.objects.Object2IntMap
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectList
import it.unimi.dsi.fastutil.objects.ObjectLists
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet

import java.util.function.BiPredicate
import java.util.function.Consumer

/** Runtime matcher for a compiled multiblock predicate grid. */
open class MultiBlockPattern private constructor(pattern: DecodedPattern) {
	@JvmField
	val aisleRepetitions: Array<IntArray> = pattern.aisleRepetitions

	@JvmField
	val unitStarts: IntArray = pattern.unitStarts

	@JvmField
	val unitDepths: IntArray = pattern.unitDepths

	@JvmField
	val structureDir: StructureDir = pattern.structureDir

	@JvmField
	val structureSlices: Array<Array<String>?>? = pattern.structureSlices

	@JvmField
	protected val blockMatches: Array<Array<Array<PatternPredicate>>?> = pattern.predicates

	@JvmField
	protected val fingerLength: Int = pattern.fingerLength

	@JvmField
	protected val thumbLength: Int = pattern.thumbLength

	@JvmField
	protected val palmLength: Int = pattern.palmLength

	@JvmField
	protected val centerOffset: CenterOffset = pattern.centerOffset

	@JvmField
	protected var formedRepetitionCount: IntArray = IntArray(pattern.aisleRepetitions.size)

	@JvmField
	var predicates = ObjectArrayList<PatternPredicate>()

	@JvmField
	var condition: PatternCondition? = null

	private var sourceDefinition: PatternDefinition? = null
	private var layoutResolver: PatternLayoutResolver? = null
	private var compiledPattern: CompiledPattern? = null
	private var runtime: PatternRuntime? = null

	fun getRepeatDescriptors(): ObjectList<PatternRepeatDescriptor> = layoutResolver?.repeatDescriptors()
		?: ObjectArrayList<PatternRepeatDescriptor>().also { out -> aisleRepetitions.forEachIndexed { i, limits -> out.add(PatternRepeatDescriptor(i, "unit-$i", "unit-$i", PatternAxis.DEPTH, limits[0], limits[1])) } }

	fun getChoiceDescriptors(): ObjectList<PatternChoiceDescriptor> = layoutResolver?.choiceDescriptors() ?: ObjectLists.emptyList()
	fun getLastMatchResult(): PatternMatchResult? = runtime?.lastResult

	/** Returns this attempt's diagnostic snapshot; machine lifecycle decisions remain with the caller. */
	fun matchPatternAt(worldState: MultiblockState, centerPos: BlockPos, frontFacing: Direction, upwardsFacing: Direction, isFlipped: Boolean, savePredicate: Boolean, expectedRepetitions: IntArray, expectedChoices: Map<String, String>): PatternMatchResult {
		expandLayout(expectedRepetitions, expectedChoices)
		runtime?.let { return it.match(worldState, centerPos, frontFacing, upwardsFacing, isFlipped, savePredicate, expectedRepetitions, expectedChoices) }
		val matched = checkPatternAtExact(worldState, centerPos, frontFacing, upwardsFacing, isFlipped, savePredicate, expectedRepetitions)
		return PatternMatchResult(matched, worldState.facts.copy(), ObjectLists.emptyList())
	}

	fun getAllPredicates(): ObjectList<PatternPredicate> = layoutResolver?.allPredicates() ?: ObjectArrayList(predicates)

	fun checkPatternAtExact(worldState: MultiblockState, centerPos: BlockPos, frontFacing: Direction, upwardsFacing: Direction, isFlipped: Boolean, savePredicate: Boolean, expectedRepetitions: IntArray, expectedChoices: Map<String, String>): Boolean {
		expandLayout(expectedRepetitions, expectedChoices)
		runtime?.let { return it.match(worldState, centerPos, frontFacing, upwardsFacing, isFlipped, savePredicate, expectedRepetitions, expectedChoices).matched }
		require(expectedChoices.isEmpty()) { "Legacy patterns do not declare choices" }
		return checkPatternAtExact(worldState, centerPos, frontFacing, upwardsFacing, isFlipped, savePredicate, expectedRepetitions)
	}

	fun checkPatternAtExact(worldState: MultiblockState, centerPos: BlockPos, frontFacing: Direction, upwardsFacing: Direction, isFlipped: Boolean, savePredicate: Boolean, expectedRepetitions: IntArray, expectedChoices: Map<String, String>, instanceRepetitions: Object2IntMap<String>): Boolean {
		expandLayout(expectedRepetitions, expectedChoices, instanceRepetitions)
		val selections = Object2ObjectOpenHashMap(expectedChoices)
		instanceRepetitions.object2IntEntrySet().forEach { selections["${it.key}#repeat"] = it.intValue.toString() }
		runtime?.let { return it.match(worldState, centerPos, frontFacing, upwardsFacing, isFlipped, savePredicate, expectedRepetitions, selections).matched }
		return checkPatternAtExact(worldState, centerPos, frontFacing, upwardsFacing, isFlipped, savePredicate, expectedRepetitions, expectedChoices)
	}

	fun expandLayout(repetitions: IntArray, choices: Map<String, String>): PatternLayout {
		layoutResolver?.let { return it.expand(repetitions, choices) }
		require(choices.isEmpty()) { "Legacy patterns do not declare choices" }
		require(repetitions.size == aisleRepetitions.size) { "Expected one repetition per unit" }
		val cells = ObjectArrayList<PatternLayoutCell>()
		var depth = getMinZ()
		for (unit in repetitions.indices) {
			require(repetitions[unit] in aisleRepetitions[unit][0]..aisleRepetitions[unit][1]) { "Repetition outside bounds" }
			repeat(repetitions[unit]) { repetition ->
				for (inner in 0 until unitDepths[unit]) {
					for (row in 0 until thumbLength) {
						for (column in 0 until palmLength) {
							cells.add(
								PatternLayoutCell("unit:$unit/$repetition/$inner/$row/$column", "$unit/$repetition", depth, row + getMinY(), column + getMinX(), getPredicate(unitStarts[unit] + inner, row, column), "", ObjectLists.emptyList(), unit, repetition, inner, row, column, "unit:$unit"),
							)
						}
					}
					depth++
				}
			}
		}
		return PatternLayout(cells)
	}

	private data class DecodedPattern(
		val predicates: Array<Array<Array<PatternPredicate>>?>,
		val structureDir: StructureDir,
		val aisleRepetitions: Array<IntArray>,
		val unitStarts: IntArray,
		val unitDepths: IntArray,
		val structureSlices: Array<Array<String>?>?,
		val centerOffset: CenterOffset,
		val fingerLength: Int,
		val thumbLength: Int,
		val palmLength: Int,
	)

	constructor(
		predicatesIn: Array<Array<Array<PatternPredicate>>>,
		structureDir: StructureDir,
		aisleRepetitions: Array<IntArray>,
		centerOffset: CenterOffset,
		fingerLength: Int,
		thumbLength: Int,
		palmLength: Int,
	) : this(
		predicatesIn,
		structureDir,
		aisleRepetitions,
		createUnitStarts(predicatesIn.size),
		createUnitDepths(predicatesIn.size),
		null,
		centerOffset,
		fingerLength,
		thumbLength,
		palmLength,
	)

	constructor(
		predicatesIn: Array<Array<Array<PatternPredicate>>>,
		structureDir: StructureDir,
		aisleRepetitions: Array<IntArray>,
		unitStarts: IntArray,
		unitDepths: IntArray,
		centerOffset: CenterOffset,
		fingerLength: Int,
		thumbLength: Int,
		palmLength: Int,
	) : this(
		predicatesIn,
		structureDir,
		aisleRepetitions,
		unitStarts,
		unitDepths,
		null,
		centerOffset,
		fingerLength,
		thumbLength,
		palmLength,
	)

	constructor(
		predicatesIn: Array<Array<Array<PatternPredicate>>>,
		structureDir: StructureDir,
		aisleRepetitions: Array<IntArray>,
		unitStarts: IntArray,
		unitDepths: IntArray,
		structureSlices: Array<Array<String>?>?,
		centerOffset: CenterOffset,
		fingerLength: Int,
		thumbLength: Int,
		palmLength: Int,
	) : this(
		DecodedPattern(
			predicatesIn.map { it }.toTypedArray(),
			structureDir,
			aisleRepetitions,
			unitStarts,
			unitDepths,
			structureSlices,
			centerOffset,
			fingerLength,
			thumbLength,
			palmLength,
		),
	)

	/** Attaches the canonical source definition used to produce this compiled matcher. */
	fun attachDefinition(definition: PatternDefinition) {
		sourceDefinition = definition
		compiledPattern = PatternCompiler().compile(definition)
		layoutResolver = compiledPattern!!.layout
		runtime = PatternRuntime(this, definition)
	}

	fun getSourceDefinition(): PatternDefinition? = sourceDefinition

	fun getCompiledPattern(): CompiledPattern? = compiledPattern

	fun expandLayout(repetitions: IntArray, choices: Map<String, String>, instanceRepetitions: Object2IntMap<String>): PatternLayout {
		layoutResolver?.let { return it.expand(repetitions, choices, instanceRepetitions) }
		require(instanceRepetitions.isEmpty()) { "Legacy patterns do not declare repeat instances" }
		return expandLayout(repetitions, choices)
	}

	internal fun expandLayoutForMatch(repetitions: IntArray, selections: Map<String, String>): PatternLayout = layoutResolver?.expandResolved(repetitions, selections) ?: expandLayout(repetitions, selections)

	fun getFingerLength(): Int = fingerLength

	fun getThumbLength(): Int = thumbLength

	fun getPalmLength(): Int = palmLength

	fun getFormedRepetitionCount(): IntArray = formedRepetitionCount

	fun checkPatternAt(worldState: MultiblockState, savePredicate: Boolean): Boolean {
		val controller = worldState.controller
		if (controller == null) {
			worldState.setError(PatternStringError("no controller found"))
			return false
		}
		val centerPos = controller.blockPos
		val frontFacing = controller.frontFacing
		val facings = if (controller.hasFrontFacing()) {
			arrayOf(frontFacing)
		} else {
			arrayOf(Direction.SOUTH, Direction.NORTH, Direction.EAST, Direction.WEST)
		}
		val upwardsFacing = controller.upwardsFacing
		val allowsFlip = controller.allowFlip()
		for (direction in facings) {
			val result = checkPatternAt(worldState, centerPos, direction, upwardsFacing, false, savePredicate)
			if (result) return true
			if (allowsFlip && checkPatternAt(worldState, centerPos, direction, upwardsFacing, true, savePredicate)) {
				return true
			}
		}
		return false
	}

	@Deprecated("Use the individual dimension getters", level = DeprecationLevel.WARNING)
	fun getDimensions(): IntArray = intArrayOf(fingerLength, thumbLength, palmLength)

	fun checkPatternAt(worldState: MultiblockState, centerPos: BlockPos, frontFacing: Direction, upwardsFacing: Direction, isFlipped: Boolean, savePredicate: Boolean): Boolean = checkPatternAtInternal(worldState, centerPos, frontFacing, upwardsFacing, isFlipped, savePredicate)

	/** Checks a pattern against explicit repetition counts without mutating the shared formed-count cache. */
	fun checkPatternAtExact(worldState: MultiblockState, centerPos: BlockPos, frontFacing: Direction, upwardsFacing: Direction, isFlipped: Boolean, savePredicate: Boolean, expectedRepetitions: IntArray): Boolean {
		runtime?.let { return it.match(worldState, centerPos, frontFacing, upwardsFacing, isFlipped, savePredicate, expectedRepetitions, emptyMap()).matched }
		require(expectedRepetitions.size == aisleRepetitions.size) {
			"Expected one repetition count per pattern unit"
		}
		expectedRepetitions.forEachIndexed { unit, repetitions ->
			val limits = aisleRepetitions[unit]
			require(repetitions in limits[0]..limits[1]) {
				"Repetition $repetitions for unit $unit is outside [${limits[0]}, ${limits[1]}]"
			}
		}
		return checkPatternAtExactInternal(
			worldState,
			centerPos,
			frontFacing,
			upwardsFacing,
			isFlipped,
			savePredicate,
			expectedRepetitions.copyOf(),
		)
	}

	private fun checkPatternAtExactInternal(worldState: MultiblockState, centerPos: BlockPos, frontFacing: Direction, upwardsFacing: Direction, isFlipped: Boolean, savePredicate: Boolean, expectedRepetitions: IntArray): Boolean {
		worldState.clean()
		val facts = worldState.facts
		val globalCount = worldState.globalCount
		val layerCount = worldState.layerCount
		val structureGlobalCount = worldState.structureGlobalCount
		val structureLayerCount = worldState.structureLayerCount
		var z = -centerOffset.maxZ()

		for (unit in expectedRepetitions.indices) {
			val unitStart = unitStarts[unit]
			val unitDepth = unitDepths[unit]
			repeat(expectedRepetitions[unit]) {
				for (inner in 0 until unitDepth) {
					layerCount.clear()
					structureLayerCount.clear()
					for (row in 0 until thumbLength) {
						val y = row - centerOffset.j()
						for (column in 0 until palmLength) {
							val x = column - centerOffset.k()
							worldState.setError(null)
							val predicate = blockMatches[unitStart + inner]!![row][column]
							val pos = setActualRelativeOffset(x, y, z, frontFacing, upwardsFacing, isFlipped)
								.offset(centerPos.x, centerPos.y, centerPos.z)
							if (!worldState.update(pos, predicate)) return false
							saveMatch(facts, worldState, predicate, pos, savePredicate)
							val canPartShared = checkPartSharing(facts, worldState, predicate)
							if (worldState.getBlockState().block is ActiveBlock) {
								facts.getOrCreate("vaBlocks") { LongOpenHashSet() }
									.add(worldState.pos.asLong())
							}
							if (!testPredicate(predicate, worldState) || !canPartShared ||
								!matchesDirectionalPredicate(predicate, worldState, frontFacing, upwardsFacing, isFlipped)
							) {
								return false
							}
							facts.getOrCreate("ioMap") { Long2ObjectOpenHashMap<Any?>() }
								.put(worldState.pos.asLong(), worldState.io)
						}
					}
					if (!checkLayerMinimums(worldState, layerCount, structureLayerCount)) return false
					z++
				}
			}
		}
		if (!checkGlobalMinimums(worldState, globalCount, structureGlobalCount)) return false
		worldState.setError(null)
		worldState.isNeededFlip = isFlipped
		return true
	}

	private fun checkPatternAtInternal(worldState: MultiblockState, centerPos: BlockPos, frontFacing: Direction, upwardsFacing: Direction, isFlipped: Boolean, savePredicate: Boolean): Boolean {
		runtime?.let {
			val matched = it.search(worldState, centerPos, frontFacing, upwardsFacing, isFlipped, savePredicate)
			if (matched) formedRepetitionCount = it.lastRepetitions.copyOf()
			return matched
		}
		var findFirstAisle = false
		var minZ = -centerOffset.maxZ()
		worldState.clean()
		val facts = worldState.facts
		val globalCount = worldState.globalCount
		val layerCount = worldState.layerCount
		val structureGlobalCount = worldState.structureGlobalCount
		val structureLayerCount = worldState.structureLayerCount
		var z = minZ++

		var unit = 0
		while (unit < aisleRepetitions.size) {
			val currentUnit = unit
			val unitStart = unitStarts[currentUnit]
			val unitDepth = unitDepths[currentUnit]
			var validRepetitions = 0
			var repetition = 0
			while (if (findFirstAisle) repetition < aisleRepetitions[currentUnit][1] else z <= -centerOffset.minZ()) {
				val repeatStartZ = z
				var restart = false
				for (inner in 0 until unitDepth) {
					layerCount.clear()
					structureLayerCount.clear()
					for (row in 0 until thumbLength) {
						val y = row - centerOffset.j()
						for (column in 0 until palmLength) {
							val x = column - centerOffset.k()
							worldState.setError(null)
							val predicate = blockMatches[unitStart + inner]!![row][column]
							val pos = setActualRelativeOffset(x, y, z, frontFacing, upwardsFacing, isFlipped)
								.offset(centerPos.x, centerPos.y, centerPos.z)
							if (!worldState.update(pos, predicate)) return false
							saveMatch(facts, worldState, predicate, pos, savePredicate)
							val canPartShared = checkPartSharing(facts, worldState, predicate)
							if (worldState.getBlockState().block is ActiveBlock) {
								facts.getOrCreate("vaBlocks") { LongOpenHashSet() }
									.add(worldState.pos.asLong())
							}
							if (!testPredicate(predicate, worldState) || !canPartShared ||
								!matchesDirectionalPredicate(predicate, worldState, frontFacing, upwardsFacing, isFlipped)
							) {
								if (findFirstAisle) {
									if (repetition < aisleRepetitions[currentUnit][0]) {
										repetition = 0
										unit = 0
										minZ++
										z = minZ
										facts.reset()
										findFirstAisle = false
									} else {
										z = repeatStartZ
									}
								} else {
									z = repeatStartZ + 1
								}
								restart = true
								break
							}
							facts.getOrCreate("ioMap") { Long2ObjectOpenHashMap<Any?>() }
								.put(worldState.pos.asLong(), worldState.io)
						}
						if (restart) break
					}
					if (restart) break
					if (!checkLayerMinimums(worldState, layerCount, structureLayerCount)) return false
					z++
				}
				if (restart) {
					repetition++
					continue
				}
				findFirstAisle = true
				validRepetitions++
				repetition++
			}
			if (repetition < aisleRepetitions[currentUnit][0] || worldState.hasError() || !findFirstAisle) {
				if (!worldState.hasError()) worldState.setError(PatternError())
				return false
			}
			formedRepetitionCount[currentUnit] = validRepetitions
			unit++
		}

		if (!checkGlobalMinimums(worldState, globalCount, structureGlobalCount)) return false
		worldState.setError(null)
		worldState.isNeededFlip = isFlipped
		return true
	}

	private fun testPredicate(predicate: PatternPredicate, state: MultiblockState): Boolean {
		val observed = predicate as? ObservedPatternPredicate
		var observation = observed?.observe(state)
		if (!(if (observation == null) predicate.test(state) else observed!!.accept(state, observation))) return false
		if (observation == null) observation = predicate.observeAccepted(state)
		for ((key, value) in observation.properties) {
			val previous: Any? = state.facts.get(key)
			if (previous != null && previous != value) {
				state.setError(PatternStringError("gtpm.multiblock.pattern.error.constraint"))
				return false
			}
		}
		observation.facts.forEach { if (!predicate.facts.contains(it)) state.facts.addFact(it) }
		observation.properties.forEach(state.facts::set)
		return true
	}

	internal fun saveMatch(facts: PatternFacts, worldState: MultiblockState, predicate: PatternPredicate, pos: BlockPos, savePredicate: Boolean) {
		if (!predicate.addCache()) return
		worldState.addPosCache(pos)
		if (savePredicate) {
			facts.getOrCreate("predicates") { Object2ObjectOpenHashMap<BlockPos, PatternPredicate>() }[pos] = predicate
		}
	}

	internal fun checkPartSharing(facts: PatternFacts, worldState: MultiblockState, predicate: PatternPredicate): Boolean {
		val part = worldState.getBlockEntity() as? IMultiPart ?: return true
		if (predicate.isAny()) return true
		if (part.isFormed && !part.hasController(worldState.controllerPos, worldState.getStructureName()) &&
			!part.canShared(worldState.lastController, worldState.getStructureName())
		) {
			worldState.setError(PatternStringError("multiblocked.pattern.error.share"))
			return false
		}
		facts.getOrCreate("parts") { ObjectOpenHashSet<IMultiPart>() }.add(part)
		return true
	}

	internal fun checkLayerMinimums(worldState: MultiblockState, layerCount: Object2IntMap<PredicateRule>, structureLayerCount: Object2IntMap<StructurePredicate>): Boolean {
		for (entry in layerCount.object2IntEntrySet()) {
			if (entry.intValue < entry.key.minLayerCount) {
				worldState.setError(SinglePredicateError(entry.key, 3))
				return false
			}
		}
		for (entry in structureLayerCount.object2IntEntrySet()) {
			val predicate = entry.key
			if (predicate is RestrictedPredicate && predicate.minCountByLayer().isPresent &&
				entry.intValue < predicate.minCountByLayer().get()
			) {
				worldState.setError(PatternStringError("gtpm.multiblock.pattern.error.limited"))
				return false
			}
		}
		return true
	}

	internal fun checkGlobalMinimums(worldState: MultiblockState, globalCount: Object2IntMap<PredicateRule>, structureGlobalCount: Object2IntMap<StructurePredicate>): Boolean {
		for (entry in globalCount.object2IntEntrySet()) {
			if (entry.intValue < entry.key.minCount) {
				worldState.setError(SinglePredicateError(entry.key, 1))
				return false
			}
		}
		for (entry in structureGlobalCount.object2IntEntrySet()) {
			val predicate = entry.key
			if (predicate is RestrictedPredicate && predicate.minCount().isPresent &&
				entry.intValue < predicate.minCount().get()
			) {
				worldState.setError(PatternStringError("gtpm.multiblock.pattern.error.limited"))
				return false
			}
		}
		return true
	}

	fun getMinZ(): Int = -centerOffset.maxZ()

	fun getMinY(): Int = -centerOffset.j()

	fun getMinX(): Int = -centerOffset.k()

	fun getPredicate(z: Int, y: Int, x: Int): PatternPredicate = blockMatches[z]!![y][x]

	fun getActualRelativeOffset(x: Int, y: Int, z: Int, facing: Direction, upwardsFacing: Direction, isFlipped: Boolean): BlockPos = setActualRelativeOffset(x, y, z, facing, upwardsFacing, isFlipped)

	fun matchesDirectionalPredicate(predicate: PatternPredicate, worldState: MultiblockState, frontFacing: Direction, upwardsFacing: Direction, isFlipped: Boolean): Boolean = matchesDirectionalPredicateInternal(predicate, worldState, frontFacing, upwardsFacing, isFlipped)

	fun resetPlacedMachineFacings(world: Level, frontFacing: Direction?, occupiedBlocks: LongOpenHashSet, machines: Long2ObjectOpenHashMap<MetaMachine>) {
		machines.long2ObjectEntrySet().fastForEach { entry ->
			val posLong = entry.longKey
			val machine = entry.value
			val pos = BlockPos.of(posLong)
			resetFacing(
				pos,
				machine.blockState,
				frontFacing,
				{ blockPos, direction ->
					!occupiedBlocks.contains(blockPos.relative(direction).asLong()) && machine.isFacingValid(direction)
				},
				{ state ->
					world.setBlock(pos, state, Block.UPDATE_CLIENTS or Block.UPDATE_KNOWN_SHAPE)
				},
			)
		}
	}

	private fun resetFacing(pos: BlockPos, blockState: BlockState, facing: Direction?, checker: BiPredicate<BlockPos, Direction>, consumer: Consumer<BlockState>) {
		if (blockState.hasProperty(BlockStateProperties.FACING)) {
			tryFacings(
				blockState,
				pos,
				checker,
				consumer,
				BlockStateProperties.FACING,
				if (facing == null) FACINGS else arrayOf(facing) + FACINGS,
			)
		} else if (blockState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
			tryFacings(
				blockState,
				pos,
				checker,
				consumer,
				BlockStateProperties.HORIZONTAL_FACING,
				if (facing == null || facing.axis == Direction.Axis.Y) FACINGS_H else arrayOf(facing) + FACINGS_H,
			)
		}
	}

	private fun tryFacings(blockState: BlockState, pos: BlockPos, checker: BiPredicate<BlockPos, Direction>, consumer: Consumer<BlockState>, property: Property<Direction>, facings: Array<Direction>) {
		var found: Direction? = null
		for (direction in facings) {
			if (checker.test(pos, direction)) {
				found = direction
				break
			}
		}
		consumer.accept(blockState.setValue(property, found ?: Direction.NORTH))
	}

	private fun matchesDirectionalPredicateInternal(predicate: PatternPredicate, worldState: MultiblockState, frontFacing: Direction, upwardsFacing: Direction, isFlipped: Boolean): Boolean {
		val direction = predicate.getDirection(worldState, frontFacing, upwardsFacing, isFlipped)
		return direction == null || matchesDirectionalState(worldState.getBlockState(), direction)
	}

	private fun setActualRelativeOffset(x: Int, y: Int, z: Int, facing: Direction, upwardsFacing: Direction, isFlipped: Boolean): BlockPos = RelativeOffset(x, y, z, structureDir, facing, upwardsFacing, isFlipped).toBlockPos()

	companion object {
		/** Keeps a compatibility grid for legacy accessors; expansion and matching retain the AST. */
		@JvmStatic
		fun fromDefinition(definition: PatternDefinition): MultiBlockPattern {
			val resolver = PatternLayoutResolver(definition)
			val descriptors = resolver.repeatDescriptors()
			val layout = resolver.expand(IntArray(descriptors.size) { descriptors[it].minimum }, emptyMap())
			require(layout.cells.isNotEmpty()) { "Pattern must contain cells" }
			val minD = layout.cells.minOf { it.depth }
			val maxD = layout.cells.maxOf { it.depth }
			val minH = layout.cells.minOf { it.height }
			val maxH = layout.cells.maxOf { it.height }
			val minW = layout.cells.minOf { it.width }
			val maxW = layout.cells.maxOf { it.width }
			val matches = Array(maxD - minD + 1) { Array(maxH - minH + 1) { Array(maxW - minW + 1) { PatternPredicates.any() } } }
			layout.cells.forEach { matches[it.depth - minD][it.height - minH][it.width - minW] = it.predicate }
			val axes = definition.axes
			val pattern =
				MultiBlockPattern(
					matches, StructureDir(RelativeDirection.valueOf(axes.width.name), RelativeDirection.valueOf(axes.height.name), RelativeDirection.valueOf(axes.depth.name)),
					Array(descriptors.size) {
						intArrayOf(descriptors[it].minimum, descriptors[it].maximum)
					},
					IntArray(descriptors.size), IntArray(descriptors.size) { 1 }, CenterOffset(-minW, -minH, -minD, -minD, -minD), matches.size, matches[0].size, matches[0][0].size,
				)
			pattern.attachDefinition(definition)
			pattern.predicates = ObjectArrayList(layout.cells.map { it.predicate })
			return pattern
		}
		private val FACINGS = arrayOf(
			Direction.SOUTH,
			Direction.NORTH,
			Direction.WEST,
			Direction.EAST,
			Direction.UP,
			Direction.DOWN,
		)
		private val FACINGS_H = arrayOf(Direction.SOUTH, Direction.NORTH, Direction.WEST, Direction.EAST)

		private fun createUnitStarts(size: Int): IntArray = IntArray(size) { it }

		private fun createUnitDepths(size: Int): IntArray = IntArray(size) { 1 }

		@JvmStatic
		fun applyDirectionalState(state: BlockState, direction: Direction): BlockState = setDirectionalState(state, direction)

		@JvmStatic
		fun matchesDirectionalState(state: BlockState, direction: Direction): Boolean {
			val property = findDirectionProperty(state, direction) ?: return false
			return state.getValue(property) == direction
		}

		private fun setDirectionalState(state: BlockState, direction: Direction): BlockState {
			val property = findDirectionProperty(state, direction) ?: return state
			return state.setValue(property, direction)
		}

		private fun findDirectionProperty(state: BlockState, direction: Direction?): DirectionProperty? = state.properties.asSequence()
			.filterIsInstance<DirectionProperty>()
			.filter { direction == null || it.possibleValues.contains(direction) }
			.minWithOrNull(compareBy { directionPropertyPriority(it) })

		private fun directionPropertyPriority(property: DirectionProperty): Int {
			val name = property.name
			if (name == "facing") return 0
			if (name == "horizontal_facing") return 1
			if (name.endsWith("_facing")) return 2
			if (name.contains("facing")) return 3
			return 4
		}
	}
}
