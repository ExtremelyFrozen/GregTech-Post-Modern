package com.gregtechceu.gtceu.api.multiblock.pattern.match

import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart
import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.api.multiblock.PatternCondition
import com.gregtechceu.gtceu.api.multiblock.RelativeOffset
import com.gregtechceu.gtceu.api.multiblock.StructureDir
import com.gregtechceu.gtceu.api.multiblock.error.PatternStringError
import com.gregtechceu.gtceu.api.multiblock.error.SinglePredicateError
import com.gregtechceu.gtceu.api.multiblock.pattern.compile.CompiledPattern
import com.gregtechceu.gtceu.api.multiblock.pattern.compile.PatternCompiler
import com.gregtechceu.gtceu.api.multiblock.pattern.layout.PatternChoiceDescriptor
import com.gregtechceu.gtceu.api.multiblock.pattern.layout.PatternLayout
import com.gregtechceu.gtceu.api.multiblock.pattern.layout.PatternRepeatDescriptor
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternDefinition
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternFacts
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.PatternPredicate
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
import it.unimi.dsi.fastutil.objects.Object2IntMap
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectList
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet

import java.util.function.BiPredicate
import java.util.function.Consumer

/** Runtime matcher for a compiled multiblock AST. */
open class MultiBlockPattern private constructor(private val sourceDefinition: PatternDefinition) {
	private val compiledPattern: CompiledPattern = PatternCompiler().compile(sourceDefinition)
	private val layoutResolver = compiledPattern.layout
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

	/** Returns this attempt's diagnostic snapshot; machine lifecycle decisions remain with the caller. */
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
		val selections = Object2ObjectOpenHashMap(expectedChoices)
		instanceRepetitions.object2IntEntrySet().forEach { selections["${it.key}#repeat"] = it.intValue.toString() }
		return runtime.match(worldState, centerPos, frontFacing, upwardsFacing, isFlipped, savePredicate, expectedRepetitions, selections).matched
	}

	fun expandLayout(repetitions: IntArray, choices: Map<String, String>): PatternLayout = layoutResolver.expand(repetitions, choices)

	fun getSourceDefinition(): PatternDefinition = sourceDefinition

	fun getCompiledPattern(): CompiledPattern = compiledPattern

	fun expandLayout(repetitions: IntArray, choices: Map<String, String>, instanceRepetitions: Object2IntMap<String>): PatternLayout = layoutResolver.expand(repetitions, choices, instanceRepetitions)

	internal fun expandLayoutForMatch(repetitions: IntArray, selections: Map<String, String>): PatternLayout = layoutResolver.expandResolved(repetitions, selections)

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
				worldState.setError(SinglePredicateError(entry.key, 3))
				return false
			}
		}
		for (entry in structureLayerCount.object2IntEntrySet()) {
			val predicate = entry.key
			if (predicate is RestrictedPredicate && predicate.minCountByLayer.isPresent &&
				entry.intValue < predicate.minCountByLayer.get()
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
			if (predicate is RestrictedPredicate && predicate.minCount.isPresent &&
				entry.intValue < predicate.minCount.get()
			) {
				worldState.setError(PatternStringError("gtpm.multiblock.pattern.error.limited"))
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
