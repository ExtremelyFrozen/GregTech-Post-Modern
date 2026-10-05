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
import com.gregtechceu.gtceu.api.multiblock.pattern.constraint.PatternConstraintEvaluator
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternDefinition
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternFacts
import com.gregtechceu.gtceu.api.multiblock.pattern.predicate.PatternPredicate
import com.gregtechceu.gtceu.api.multiblock.pattern.predicate.PredicateRule
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.RestrictedPredicate
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.StructurePredicate

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
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet

import java.util.function.BiPredicate
import java.util.function.Consumer
import java.util.function.Supplier

/** Runtime matcher for a compiled multiblock predicate grid. */
open class MultiBlockPattern private constructor(pattern: DecodedPattern) {
	@JvmField
	val aisleRepetitions: Array<IntArray>

	@JvmField
	val unitStarts: IntArray

	@JvmField
	val unitDepths: IntArray

	@JvmField
	val structureDir: StructureDir

	@JvmField
	val structureSlices: Array<Array<String>?>?

	@JvmField
	protected val blockMatches: Array<Array<Array<PatternPredicate>>?>?

	@JvmField
	protected val fingerLength: Int

	@JvmField
	protected val thumbLength: Int

	@JvmField
	protected val palmLength: Int

	@JvmField
	protected val centerOffset: CenterOffset

	@JvmField
	protected var formedRepetitionCount: IntArray

	@JvmField
	var predicates: MutableCollection<PatternPredicate> = ObjectArrayList<PatternPredicate>()

	@JvmField
	var condition: PatternCondition? = null

	private var sourceDefinition: PatternDefinition? = null

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

	init {
		blockMatches = pattern.predicates
		this.structureDir = pattern.structureDir
		this.aisleRepetitions = pattern.aisleRepetitions
		this.unitStarts = pattern.unitStarts
		this.unitDepths = pattern.unitDepths
		this.structureSlices = pattern.structureSlices
		this.formedRepetitionCount = IntArray(pattern.aisleRepetitions.size)
		this.centerOffset = pattern.centerOffset
		this.fingerLength = pattern.fingerLength
		this.thumbLength = pattern.thumbLength
		this.palmLength = pattern.palmLength
	}

	/** Attaches the canonical source definition used to produce this compiled matcher. */
	fun attachDefinition(definition: PatternDefinition) {
		sourceDefinition = definition
	}

	fun getSourceDefinition(): PatternDefinition? = sourceDefinition

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
		val facts = worldState.getFacts()
		val globalCount = worldState.getGlobalCount()
		val layerCount = worldState.getLayerCount()
		val structureGlobalCount = worldState.getStructureGlobalCount()
		val structureLayerCount = worldState.getStructureLayerCount()
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
							val predicate = blockMatches!![unitStart + inner]!![row][column]
							val pos = setActualRelativeOffset(x, y, z, frontFacing, upwardsFacing, isFlipped)
								.offset(centerPos.x, centerPos.y, centerPos.z)
							if (!worldState.update(pos, predicate)) return false
							saveMatch(facts, worldState, predicate, pos, savePredicate)
							val canPartShared = checkPartSharing(facts, worldState, predicate)
							if (worldState.getBlockState().block is ActiveBlock) {
								facts.getOrCreate("vaBlocks", Supplier { LongOpenHashSet() })
									.add(worldState.getPos().asLong())
							}
							if (!predicate.test(worldState) || !canPartShared ||
								!matchesDirectionalPredicate(predicate, worldState, frontFacing, upwardsFacing, isFlipped)
							) {
								return false
							}
							facts.getOrCreate("ioMap", Supplier { Long2ObjectOpenHashMap<Any?>() })
								.put(worldState.getPos().asLong(), worldState.io)
						}
					}
					if (!checkLayerMinimums(worldState, layerCount, structureLayerCount)) return false
					z++
				}
			}
		}
		if (!checkGlobalMinimums(worldState, globalCount, structureGlobalCount)) return false
		if (sourceDefinition != null && !PatternConstraintEvaluator.satisfied(sourceDefinition!!, facts)) {
			worldState.setError(PatternStringError("gtpm.multiblock.pattern.error.constraint"))
			return false
		}
		worldState.setError(null)
		worldState.setNeededFlip(isFlipped)
		return true
	}

	private fun checkPatternAtInternal(worldState: MultiblockState, centerPos: BlockPos, frontFacing: Direction, upwardsFacing: Direction, isFlipped: Boolean, savePredicate: Boolean): Boolean {
		var findFirstAisle = false
		var minZ = -centerOffset.maxZ()
		worldState.clean()
		val facts = worldState.getFacts()
		val globalCount = worldState.getGlobalCount()
		val layerCount = worldState.getLayerCount()
		val structureGlobalCount = worldState.getStructureGlobalCount()
		val structureLayerCount = worldState.getStructureLayerCount()
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
							val predicate = blockMatches!![unitStart + inner]!![row][column]
							val pos = setActualRelativeOffset(x, y, z, frontFacing, upwardsFacing, isFlipped)
								.offset(centerPos.x, centerPos.y, centerPos.z)
							if (!worldState.update(pos, predicate)) return false
							saveMatch(facts, worldState, predicate, pos, savePredicate)
							val canPartShared = checkPartSharing(facts, worldState, predicate)
							if (worldState.getBlockState().block is ActiveBlock) {
								facts.getOrCreate("vaBlocks", Supplier { LongOpenHashSet() })
									.add(worldState.getPos().asLong())
							}
							if (!predicate.test(worldState) || !canPartShared ||
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
							facts.getOrCreate("ioMap", Supplier { Long2ObjectOpenHashMap<Any?>() })
								.put(worldState.getPos().asLong(), worldState.io)
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
		if (sourceDefinition != null && !PatternConstraintEvaluator.satisfied(sourceDefinition!!, facts)) {
			worldState.setError(PatternStringError("gtpm.multiblock.pattern.error.constraint"))
			return false
		}
		worldState.setError(null)
		worldState.setNeededFlip(isFlipped)
		return true
	}

	private fun saveMatch(facts: PatternFacts, worldState: MultiblockState, predicate: PatternPredicate, pos: BlockPos, savePredicate: Boolean) {
		if (!predicate.addCache()) return
		worldState.addPosCache(pos)
		if (savePredicate) {
			facts.getOrCreate("predicates", Supplier { Object2ObjectOpenHashMap<BlockPos, PatternPredicate>() })
				.put(pos, predicate)
		}
	}

	private fun checkPartSharing(facts: PatternFacts, worldState: MultiblockState, predicate: PatternPredicate): Boolean {
		val part = worldState.getBlockEntity() as? IMultiPart ?: return true
		if (predicate.isAny()) return true
		if (part.isFormed && !part.hasController(worldState.controllerPos, worldState.getStructureName()) &&
			!part.canShared(worldState.lastController, worldState.getStructureName())
		) {
			worldState.setError(PatternStringError("multiblocked.pattern.error.share"))
			return false
		}
		facts.getOrCreate("parts", Supplier { ObjectOpenHashSet<IMultiPart>() }).add(part)
		return true
	}

	private fun checkLayerMinimums(worldState: MultiblockState, layerCount: Object2IntMap<PredicateRule>, structureLayerCount: Object2IntMap<StructurePredicate>): Boolean {
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

	private fun checkGlobalMinimums(worldState: MultiblockState, globalCount: Object2IntMap<PredicateRule>, structureGlobalCount: Object2IntMap<StructurePredicate>): Boolean {
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

	fun getPredicate(z: Int, y: Int, x: Int): PatternPredicate = blockMatches!![z]!![y][x]

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
				BiPredicate { blockPos, direction ->
					!occupiedBlocks.contains(blockPos.relative(direction).asLong()) && machine.isFacingValid(direction)
				},
				Consumer { state ->
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
