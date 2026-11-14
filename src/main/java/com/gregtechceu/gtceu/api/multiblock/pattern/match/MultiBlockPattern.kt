package com.gregtechceu.gtceu.api.multiblock.pattern.match

import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart
import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.api.multiblock.PatternCondition
import com.gregtechceu.gtceu.api.multiblock.RelativeOffset
import com.gregtechceu.gtceu.api.multiblock.StructureDir
import com.gregtechceu.gtceu.api.multiblock.error.PatternStringError
import com.gregtechceu.gtceu.api.multiblock.error.StructureIssue
import com.gregtechceu.gtceu.api.multiblock.pattern.compile.CompiledPattern
import com.gregtechceu.gtceu.api.multiblock.pattern.compile.PatternCompiler
import com.gregtechceu.gtceu.api.multiblock.pattern.layout.PatternChoiceDescriptor
import com.gregtechceu.gtceu.api.multiblock.pattern.layout.PatternLayout
import com.gregtechceu.gtceu.api.multiblock.pattern.layout.PatternRepeatDescriptor
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternDefinition
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternFacts
import com.gregtechceu.gtceu.api.multiblock.pattern.model.measurement.PatternMeasurement
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.PatternPredicate
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.PatternPredicateCompiler
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.PredicateRule
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
import it.unimi.dsi.fastutil.longs.LongSet
import it.unimi.dsi.fastutil.longs.LongSets
import it.unimi.dsi.fastutil.objects.Object2IntMap
import it.unimi.dsi.fastutil.objects.Object2IntMaps
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectList
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet

import java.util.function.BiPredicate
import java.util.function.Consumer

/** Runtime matcher for a compiled multiblock AST. */
open class MultiBlockPattern private constructor(private val sourceDefinition: PatternDefinition) {
	private val compiledPattern: CompiledPattern = PatternCompiler().compile(sourceDefinition)
	private val layoutResolver = compiledPattern.layout
	private val measurementRules = Array(sourceDefinition.measurements.size) { PatternPredicateCompiler.compile(sourceDefinition.measurements[it].boundary) }
	private val cachedSearchRadius by lazy { layoutResolver.searchRadius() }
	private val eventDriven by lazy { layoutResolver.allPredicates().all { it.hasPositionDependencies() } }
	private val runtime = PatternRuntime(this, sourceDefinition)

	private val minimumDepth = compiledPattern.cells.minOf { it.depth }
	private val minimumHeight = compiledPattern.cells.minOf { it.height }
	private val minimumWidth = compiledPattern.cells.minOf { it.width }

	@JvmField
	val structureDir: StructureDir = sourceDefinition.axes.let {
		StructureDir(RelativeDirection.valueOf(it.width.name), RelativeDirection.valueOf(it.height.name), RelativeDirection.valueOf(it.depth.name))
	}

	@JvmField
	protected val fingerLength: Int = compiledPattern.cells.maxOf { it.depth } - minimumDepth + 1

	@JvmField
	protected val thumbLength: Int = compiledPattern.cells.maxOf { it.height } - minimumHeight + 1

	@JvmField
	protected val palmLength: Int = compiledPattern.cells.maxOf { it.width } - minimumWidth + 1

	@JvmField
	protected var formedRepetitionCount: IntArray = IntArray(layoutResolver.repeatDescriptors().size)

	@JvmField
	var condition: PatternCondition? = null

	fun getRepeatDescriptors(): ObjectList<PatternRepeatDescriptor> = layoutResolver.repeatDescriptors()

	fun getChoiceDescriptors(): ObjectList<PatternChoiceDescriptor> = layoutResolver.choiceDescriptors()

	fun getLastMatchResult(): PatternMatchResult? = runtime.lastResult

	/** Returns this attempt's diagnostics; machine lifecycle decisions remain with the caller. */
	fun matchPatternAt(worldState: MultiblockState, centerPos: BlockPos, frontFacing: Direction, upwardsFacing: Direction, isFlipped: Boolean, savePredicate: Boolean, expectedRepetitions: IntArray, expectedChoices: Map<String, String>): PatternMatchResult {
		expandLayout(expectedRepetitions, expectedChoices)
		return runtime.match(worldState, centerPos, frontFacing, upwardsFacing, isFlipped, savePredicate, expectedRepetitions, expectedChoices)
	}

	fun getAllPredicates(): ObjectList<PatternPredicate> = layoutResolver.allPredicates()

	fun checkPatternAtExact(worldState: MultiblockState, centerPos: BlockPos, frontFacing: Direction, upwardsFacing: Direction, isFlipped: Boolean, savePredicate: Boolean, expectedRepetitions: IntArray, expectedChoices: Map<String, String>): Boolean {
		expandLayout(expectedRepetitions, expectedChoices)
		return runtime.match(worldState, centerPos, frontFacing, upwardsFacing, isFlipped, savePredicate, expectedRepetitions, expectedChoices).matched
	}

	fun checkPatternAtExact(worldState: MultiblockState, centerPos: BlockPos, frontFacing: Direction, upwardsFacing: Direction, isFlipped: Boolean, savePredicate: Boolean, expectedRepetitions: IntArray, expectedChoices: Map<String, String>, instanceRepetitions: Object2IntMap<String>): Boolean {
		expandLayout(expectedRepetitions, expectedChoices, instanceRepetitions)
		var selections = PatternSelections.of(expectedChoices)
		instanceRepetitions.object2IntEntrySet().forEach { selections = selections.with("${it.key}#repeat", it.intValue.toString()) }
		return runtime.match(worldState, centerPos, frontFacing, upwardsFacing, isFlipped, savePredicate, expectedRepetitions, selections).matched
	}

	fun expandLayout(repetitions: IntArray, choices: Map<String, String>): PatternLayout = layoutResolver.expand(repetitions, choices, Object2IntMaps.emptyMap(), getMeasurementValues(choices))

	/** User selections are checked once before expansion; measured matching values come from world probes. */
	fun getMeasurementValues(choices: Map<String, String>): Object2IntMap<String> = Object2IntOpenHashMap<String>().also { values ->
		for (measurement in getMeasurements()) {
			val selected = choices["@measure:${measurement.parameter}"]
			val value = selected?.toInt() ?: measurement.minimum
			values[measurement.parameter] = value
		}
	}

	/** Expands an exact interpretation, with explicitly supplied measured parameter values. */
	fun expandLayout(repetitions: IntArray, choices: Map<String, String>, instanceRepetitions: Object2IntMap<String>, parameters: Object2IntMap<String>): PatternLayout = layoutResolver.expand(repetitions, choices, instanceRepetitions, parameters)

	internal fun expandResolvedLayout(repetitions: IntArray, selections: Map<String, String>, parameters: Object2IntMap<String>): PatternLayout = layoutResolver.expandResolved(repetitions, selections, parameters)

	/** Returns depth, height and width without enumerating layout cells. */
	fun getDimensions(repetitions: IntArray, choices: Map<String, String>): IntArray = layoutResolver.dimensions(repetitions, choices, getMeasurementValues(choices))

	/** Converts an actual dimension back into the nearest attainable repeat count. */
	fun resizeLayout(repetitions: IntArray, choices: Map<String, String>, index: Int, size: Int): IntArray = layoutResolver.resize(repetitions, choices, index, size)

	fun getSourceDefinition(): PatternDefinition = sourceDefinition

	fun getMeasurements(): ObjectList<PatternMeasurement> = layoutResolver.measurements()

	internal fun measurementRule(index: Int): PatternPredicate = measurementRules[index]

	fun getSearchRadius(): IntArray = cachedSearchRadius

	fun isEventDriven(): Boolean = eventDriven

	fun getCompiledPattern(): CompiledPattern = compiledPattern

	fun expandLayout(repetitions: IntArray, choices: Map<String, String>, instanceRepetitions: Object2IntMap<String>): PatternLayout = layoutResolver.expand(repetitions, choices, instanceRepetitions, getMeasurementValues(choices))

	internal fun expandLayoutForMatch(repetitions: IntArray, selections: Map<String, String>): PatternLayout = layoutResolver.expandResolved(repetitions, selections)

	internal suspend fun expandLayoutForMatch(work: PatternWork<*>, repetitions: IntArray, selections: Map<String, String>): PatternLayout = layoutResolver.expandResolved(work, repetitions, selections)

	internal suspend fun expandLayoutForMatch(work: PatternWork<*>, repetitions: IntArray, selections: Map<String, String>, parameters: Object2IntMap<String>): PatternLayout = layoutResolver.expandResolved(work, repetitions, selections, parameters)

	/** Creates a resumable server-thread search whose intermediate state is isolated from the machine. */
	fun beginExactCheck(worldState: MultiblockState, front: Direction, up: Direction, flipped: Boolean, repetitions: IntArray, choices: Map<String, String>): PatternCheckSession {
		val candidate = MultiblockState(worldState.world, worldState.controllerPos, worldState.structureName)
		candidate.lastController = worldState.controller
		val matcher = PatternRuntime(this, sourceDefinition)
		val work = PatternWork { matcher.matchExact(this, candidate, worldState.controllerPos, front, up, flipped, true, repetitions, choices).matched }
		return PatternCheckSession(candidate, work, worldState)
	}

	/** Creates an isolated search, with world capture and publication confined to server-thread drains. */
	@JvmOverloads
	fun beginCheck(worldState: MultiblockState, changedPositions: LongSet = LongSets.emptySet()): PatternCheckSession {
		val candidate = MultiblockState(worldState.world, worldState.controllerPos, worldState.structureName)
		val controller = worldState.controller
		candidate.lastController = controller
		val previous = worldState.matchCache?.takeIf { it.pattern === this }
		val check = PatternWork {
			if (controller == null) {
				candidate.setError(PatternStringError("no controller found"))
				false
			} else {
				candidate.clean()
				if (previous != null && getMeasurements().isEmpty() && getChoiceDescriptors().isEmpty() && getRepeatDescriptors().all { it.minimum == it.maximum } && previous.eventDriven && !worldState.hasError() && changedPositions.isNotEmpty() &&
					previous.front == (if (controller.hasFrontFacing()) controller.frontFacing else Direction.SOUTH) && previous.up == controller.upwardsFacing && !previous.flipped &&
					previous.checkLocal(this, candidate, changedPositions)
				) {
					candidate.applyCachedMatch(worldState)
					return@PatternWork true
				}
				val matcher = PatternRuntime(this@MultiBlockPattern, sourceDefinition, previous, changedPositions)
				val facings = if (controller.hasFrontFacing()) arrayOf(controller.frontFacing) else arrayOf(Direction.SOUTH, Direction.NORTH, Direction.EAST, Direction.WEST)
				val upwardsFacing = controller.upwardsFacing
				val allowFlip = controller.allowFlip()
				var matched = false
				val failures = PatternFailureSelection()
				for (facing in facings) {
					if (matcher.search(this, candidate, controller.blockPos, facing, upwardsFacing, false, false)) {
						matched = true
						break
					}
					failures.include(candidate)
					if (allowFlip && matcher.search(this, candidate, controller.blockPos, facing, upwardsFacing, true, false)) {
						matched = true
						break
					}
					if (allowFlip) failures.include(candidate)
				}
				if (!matched) failures.apply(candidate)
				matched
			}
		}
		return PatternCheckSession(candidate, check, worldState)
	}

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
		val failures = PatternFailureSelection()
		for (direction in facings) {
			val result = checkPatternAt(worldState, centerPos, direction, upwardsFacing, false, savePredicate)
			if (result) return true
			failures.include(worldState)
			if (allowsFlip && checkPatternAt(worldState, centerPos, direction, upwardsFacing, true, savePredicate)) {
				return true
			}
			if (allowsFlip) failures.include(worldState)
		}
		failures.apply(worldState)
		return false
	}

	fun checkPatternAt(worldState: MultiblockState, centerPos: BlockPos, frontFacing: Direction, upwardsFacing: Direction, isFlipped: Boolean, savePredicate: Boolean): Boolean = checkPatternAtInternal(worldState, centerPos, frontFacing, upwardsFacing, isFlipped, savePredicate)

	/** Checks a pattern against explicit repetition counts without mutating the shared formed-count cache. */
	fun checkPatternAtExact(worldState: MultiblockState, centerPos: BlockPos, frontFacing: Direction, upwardsFacing: Direction, isFlipped: Boolean, savePredicate: Boolean, expectedRepetitions: IntArray): Boolean =
		runtime.match(worldState, centerPos, frontFacing, upwardsFacing, isFlipped, savePredicate, expectedRepetitions, emptyMap()).matched

	private fun checkPatternAtInternal(worldState: MultiblockState, centerPos: BlockPos, frontFacing: Direction, upwardsFacing: Direction, isFlipped: Boolean, savePredicate: Boolean): Boolean {
		val matched = runtime.search(worldState, centerPos, frontFacing, upwardsFacing, isFlipped, savePredicate)
		if (matched) formedRepetitionCount = runtime.lastRepetitions.copyOf()
		return matched
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
				worldState.setError(StructureIssue.count(null, StructureIssue.Count.LAYER_MINIMUM, entry.key.description(worldState), entry.key.minLayerCount, entry.intValue))
				return false
			}
		}
		for (entry in structureLayerCount.object2IntEntrySet()) {
			val predicate = entry.key
			if (predicate is RestrictedPredicate && predicate.minCountByLayer.isPresent &&
				entry.intValue < predicate.minCountByLayer.get()
			) {
				worldState.setError(StructureIssue.count(null, StructureIssue.Count.LAYER_MINIMUM, predicate.description(worldState), predicate.minCountByLayer.get(), entry.intValue))
				return false
			}
		}
		return true
	}

	internal fun checkGlobalMinimums(worldState: MultiblockState, globalCount: Object2IntMap<PredicateRule>, structureGlobalCount: Object2IntMap<StructurePredicate>): Boolean {
		for (entry in globalCount.object2IntEntrySet()) {
			if (entry.intValue < entry.key.minCount) {
				worldState.setError(StructureIssue.count(null, StructureIssue.Count.MINIMUM, entry.key.description(worldState), entry.key.minCount, entry.intValue))
				return false
			}
		}
		for (entry in structureGlobalCount.object2IntEntrySet()) {
			val predicate = entry.key
			if (predicate is RestrictedPredicate && predicate.minCount.isPresent &&
				entry.intValue < predicate.minCount.get()
			) {
				worldState.setError(StructureIssue.count(null, StructureIssue.Count.MINIMUM, predicate.description(worldState), predicate.minCount.get(), entry.intValue))
				return false
			}
		}
		return true
	}

	fun getMinZ(): Int = minimumDepth

	fun getMinY(): Int = minimumHeight

	fun getMinX(): Int = minimumWidth

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
		@JvmStatic
		fun fromDefinition(definition: PatternDefinition): MultiBlockPattern = MultiBlockPattern(definition)

		private val FACINGS = arrayOf(
			Direction.SOUTH,
			Direction.NORTH,
			Direction.WEST,
			Direction.EAST,
			Direction.UP,
			Direction.DOWN,
		)
		private val FACINGS_H = arrayOf(Direction.SOUTH, Direction.NORTH, Direction.WEST, Direction.EAST)

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
