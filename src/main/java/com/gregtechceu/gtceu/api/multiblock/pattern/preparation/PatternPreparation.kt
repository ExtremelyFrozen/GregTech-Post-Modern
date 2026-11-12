package com.gregtechceu.gtceu.api.multiblock.pattern.preparation

import com.gregtechceu.gtceu.api.block.ActiveBlock
import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart
import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.api.multiblock.error.PatternStringError
import com.gregtechceu.gtceu.api.multiblock.pattern.layout.PatternAxis
import com.gregtechceu.gtceu.api.multiblock.pattern.layout.PatternLayout
import com.gregtechceu.gtceu.api.multiblock.pattern.match.MultiBlockPattern
import com.gregtechceu.gtceu.api.multiblock.pattern.match.PatternCoordinate
import com.gregtechceu.gtceu.api.multiblock.pattern.match.PatternDiagnostic
import com.gregtechceu.gtceu.api.multiblock.pattern.match.PatternMatchCache
import com.gregtechceu.gtceu.api.multiblock.pattern.match.PatternMatchResult
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternConstraint
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternDefinition
import com.gregtechceu.gtceu.config.ConfigHolder

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import it.unimi.dsi.fastutil.longs.LongArrays
import it.unimi.dsi.fastutil.longs.LongComparator
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.longs.LongSet
import it.unimi.dsi.fastutil.longs.LongSets
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectLists
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import it.unimi.dsi.fastutil.objects.ObjectSet

/** Keeps world-owned objects on the caller thread while publishing an independent worker input. */
class PatternPreparation private constructor(
	private val pattern: MultiBlockPattern,
	private val layout: PatternLayout,
	private val target: MultiblockState,
	private val center: BlockPos,
	private val front: Direction,
	private val up: Direction,
	private val flipped: Boolean,
	private val save: Boolean,
	private val repetitions: IntArray,
	private val selections: Object2ObjectMap<String, String>,
	private val captures: Array<CapturedPatternCell>,
	private val capturedBlocks: CapturedPatternBlocks,
	private val compiler: PreparedRuleCompiler,
	private val configFlags: Int,
	val input: PreparedPatternInput,
) {
	private var confirmed: Array<PreparedGroupContribution>? = null
	private val dependencyCells = Long2ObjectOpenHashMap<IntArrayList>().also { index ->
		for (cell in captures.indices) {
			for (position in captures[cell].dependencies) index.computeIfAbsent(position) { IntArrayList() }.add(cell)
		}
	}

	/** Restores an evaluated selection once. Matching and position observers are never executed here. */
	fun apply(result: PreparedPatternResult): MultiblockState {
		val state = MultiblockState(target.world, target.controllerPos, target.structureName)
		state.lastController = target.lastController
		state.inheritReadTracking(target)
		state.clean()
		state.isNeededFlip = flipped
		val facts = state.facts
		val instances = Object2IntOpenHashMap<String>()
		for ((key, value) in selections) if (key.endsWith("#repeat")) instances[key.removeSuffix("#repeat")] = value.toInt()
		facts.set("pattern.choices", selections)
		facts.set("pattern.instance_repetitions", instances)
		facts.set("pattern.repetitions", repetitions.copyOf())
		facts.set("pattern.width", layout.size(PatternAxis.WIDTH))
		facts.set("pattern.height", layout.size(PatternAxis.HEIGHT))
		facts.set("pattern.depth", layout.size(PatternAxis.DEPTH))

		var lastEffectCell = -1
		for ((cell, leaf, renderMask, slot) in result.effects) {
			val capture = captures[cell]
			if (lastEffectCell != cell) {
				state.updateCached(capture.position, layout.cells[cell].predicate, capture.block!!, capture.entity)
				lastEffectCell = cell
			}
			if (leaf >= 0) compiler.applyLeaf(leaf, state)
			if (renderMask) facts.getOrCreate("renderMask") { LongOpenHashSet() }.add(capture.position.asLong())
			slot?.let { name ->
				val slots: Long2ObjectArrayMap<ObjectSet<String>> = facts.getOrCreate("slots") { Long2ObjectArrayMap() }
				slots.computeIfAbsent(capture.position.asLong()) { ObjectOpenHashSet() }.add(name)
			}
		}
		compiler.restoreCounts(result, state)
		for ((fact, count) in result.factCounts) facts.addFactCount(fact, count)
		facts.ensureScopes(ObjectArrayList.wrap(result.scopes))
		for ((scope, fact, count) in result.scopedCounts) facts.addScopedCount(scope, fact, count)
		for ((key, _, reference) in result.properties) facts.set(key, compiler.value(reference))
		val cache = PatternMatchCache(pattern, layout, center, front, up, flipped, repetitions.copyOf(), selections)
		for ((cell1, observation, io) in result.accepted) {
			val capture = captures[cell1]
			val cell = layout.cells[cell1]
			state.updateCached(capture.position, cell.predicate, capture.block!!, capture.entity)
			state.io = decodeIO(io)
			state.addPosCache(capture.position)
			if (capture.part != null && !cell.predicate.isAny()) facts.getOrCreate("parts") { ObjectOpenHashSet<IMultiPart>() }.add(capture.part)
			pattern.saveMatch(facts, state, cell.predicate, capture.position, save)
			if (capture.block.block is ActiveBlock) facts.getOrCreate("vaBlocks") { LongOpenHashSet() }.add(capture.position.asLong())
			facts.getOrCreate("ioMap") { Long2ObjectOpenHashMap<IO?>() }[capture.position.asLong()] = state.io
			cache.add(state, cell, compiler.restore(observation))
		}
		// A failed position is part of the observed frontier, even when it contributed no successful cell.
		for (capture in captures) for (dependency in capture.dependencies) state.addPosCache(BlockPos.of(dependency))
		captures.getOrNull(result.lastCell)?.let { capture ->
			val predicate = layout.cells[result.lastCell].predicate
			if (capture.block == null) state.update(capture.position, predicate) else state.updateCached(capture.position, predicate, capture.block, capture.entity)
			state.io = decodeIO(result.lastIO)
		}
		val diagnostics = ObjectArrayList<PatternDiagnostic>(result.problems.size)
		for ((cell, path, repeatPath, depth, height, width, position, expected, missingFact, difference) in result.problems) {
			val capture = captures.getOrNull(cell)
			val diagnostic = PatternDiagnostic(
				path,
				repeatPath,
				PatternCoordinate(depth, height, width),
				BlockPos.of(position),
				capturedBlocks[position]?.block,
				expected,
				missingFact,
				difference,
			)
			diagnostics.add(if (capture != null && expected == layout.cells[cell].token) layout.cells[cell].predicate.diagnose(diagnostic) else diagnostic)
		}
		val match = PatternMatchResult(result.matched, facts, ObjectLists.unmodifiable(diagnostics))
		facts.set("pattern.diagnostics", match.diagnostics)
		state.matchResult = match
		if (result.matched) {
			confirmed = result.contributions
			cache.preparation = this
			state.matchCache = cache
		}
		state.setError(if (result.matched) null else PatternStringError("gtpm.multiblock.pattern.error.constraint"))
		target.applyMatch(state)
		return target
	}

	companion object {
		/** Returns null before world capture if any runtime is not one of the explicitly adapted built-ins. */
		@JvmStatic
		fun prepare(
			pattern: MultiBlockPattern,
			definition: PatternDefinition,
			layout: PatternLayout,
			target: MultiblockState,
			center: BlockPos,
			front: Direction,
			up: Direction,
			flipped: Boolean,
			save: Boolean,
			repetitions: IntArray,
			selections: Object2ObjectMap<String, String>,
			previous: PatternPreparation? = null,
			changed: LongSet = LongSets.emptySet(),
			bounds: PatternLayout = layout,
			diagnostic: Boolean = false,
			collection: PatternWorldCollection? = null,
		): PatternPreparation? {
			if (layout.rejections.isNotEmpty()) return null
			for (index in layout.cells.indices) if (!PreparedRuleCompiler.trusted(layout.predicate(index), diagnostic)) return null
			val flags = (if (ConfigHolder.INSTANCE.machines.enableMaintenance) 1 else 0) or (if (ConfigHolder.INSTANCE.machines.enableResearch) 2 else 0)
			val reusable = previous?.takeIf {
				changed.isNotEmpty() && it.confirmed != null && it.pattern === pattern && it.layout === layout && it.target.world === target.world &&
					it.target.controllerPos == target.controllerPos && it.target.structureName == target.structureName &&
					it.center == center && it.front == front && it.up == up && it.flipped == flipped && it.save == save &&
					it.repetitions.contentEquals(repetitions) && it.selections == selections && it.configFlags == flags &&
					!changed.contains(target.controllerPos.asLong()) && it.compiler.nodeCount <= maxOf(4096, layout.cells.size * 32)
			}
			val affected = BooleanArray(layout.cells.size) { reusable == null }
			if (reusable != null) {
				val changedPositions = changed.iterator()
				while (changedPositions.hasNext()) {
					val cells = reusable.dependencyCells[changedPositions.nextLong()] ?: continue
					for (index in cells.indices) affected[cells.getInt(index)] = true
				}
			}
			var hasAffectedCells = false
			for (affectedCell in affected) {
				if (affectedCell) {
					hasAffectedCells = true
					break
				}
			}
			val width = pattern.getActualRelativeOffset(1, 0, 0, front, up, flipped)
			val height = pattern.getActualRelativeOffset(0, 1, 0, front, up, flipped)
			val depth = pattern.getActualRelativeOffset(0, 0, 1, front, up, flipped)
			fun position(x: Int, y: Int, z: Int): BlockPos = BlockPos(center.x + x * width.x + y * height.x + z * depth.x, center.y + x * width.y + y * height.y + z * depth.y, center.z + x * width.z + y * height.z + z * depth.z)
			val positions = Array(layout.cells.size) { position(layout.width(it), layout.height(it), layout.depth(it)) }
			val blocks = if (reusable != null && !hasAffectedCells) reusable.capturedBlocks else CapturedPatternBlocks(reusable?.capturedBlocks)
			fun record(pos: BlockPos) {
				target.recordRead(pos)
				target.addPosCache(pos)
			}
			fun capture(pos: BlockPos, refresh: Boolean = false) {
				record(pos)
				if (!refresh && blocks[pos.asLong()] != null) return
				if (collection != null) {
					blocks[pos.asLong()] = collection.read(target, pos)
					return
				}
				val loaded = target.world.isLoaded(pos)
				val block = if (loaded) target.world.getBlockState(pos) else null
				blocks[pos.asLong()] = CapturedPatternBlock(block, if (block?.hasBlockEntity() == true) target.world.getBlockEntity(pos) else null)
			}
			// Section locality affects capture only; the instruction array retains declared layout order.
			// Every reused frontier is subscribed by the new attempt before it consumes the old input.
			reusable?.capturedBlocks?.registerReads(target)
			val refreshed = LongOpenHashSet(positions.size)
			for (index in positions.indices) if (affected[index]) refreshed.add(positions[index].asLong())
			val orderedPositions = refreshed.toLongArray()
			LongArrays.quickSort(orderedPositions, READ_ORDER)
			for (packed in orderedPositions) capture(BlockPos.of(packed), true)
			record(target.controllerPos)
			val scratch = MultiblockState(target.world, target.controllerPos, target.structureName)
			scratch.lastController = target.lastController
			scratch.inheritReadTracking(target)
			scratch.clean()
			scratch.isNeededFlip = flipped
			scratch.facts.set("pattern.width", bounds.size(PatternAxis.WIDTH))
			scratch.facts.set("pattern.height", bounds.size(PatternAxis.HEIGHT))
			scratch.facts.set("pattern.depth", bounds.size(PatternAxis.DEPTH))
			val dependencies = Array(positions.size) { index -> if (reusable != null && !affected[index]) LongOpenHashSet(reusable.captures[index].dependencies) else LongOpenHashSet() }
			for (index in positions.indices) {
				if (!affected[index]) continue
				val block = blocks[positions[index].asLong()]!!
				dependencies[index].add(positions[index].asLong())
				if (block.block != null) {
					scratch.updateCached(positions[index], layout.predicate(index), block.block, block.entity)
					PreparedRuleCompiler.dependencies(layout.predicate(index), scratch, dependencies[index])
				}
			}
			val dependencyPositions = LongOpenHashSet()
			for (index in dependencies.indices) {
				if (affected[index]) {
					val entries = dependencies[index].iterator()
					while (entries.hasNext()) dependencyPositions.add(entries.nextLong())
				}
			}
			val orderedDependencies = dependencyPositions.toLongArray()
			LongArrays.quickSort(orderedDependencies, READ_ORDER)
			for (packed in orderedDependencies) capture(BlockPos.of(packed), reusable != null && changed.contains(packed) && !refreshed.contains(packed))
			val compiler = if (reusable != null && !hasAffectedCells) reusable.compiler else reusable?.compiler?.extend(blocks) ?: PreparedRuleCompiler(blocks)
			val captures = Array(positions.size) { index ->
				if (reusable != null && !affected[index]) return@Array reusable.captures[index]
				val block = blocks[positions[index].asLong()]!!
				CapturedPatternCell(positions[index], block.block, block.entity, block.entity as? IMultiPart, dependencies[index].toLongArray())
			}
			val cells = Array(positions.size) { index ->
				if (reusable != null && !affected[index]) return@Array reusable.input.cells[index]
				val cell = layout.cells[index]
				val captured = captures[index]
				if (captured.block != null) scratch.updateCached(captured.position, cell.predicate, captured.block, captured.entity)
				val root = compiler.compile(cell.predicate, scratch, captured.block != null)
				val part = captured.part
				val shareable = part == null || cell.predicate.isAny() || !part.isFormed || part.hasController(target.controllerPos, target.structureName) || part.canShared(scratch.lastController, target.structureName)
				val directional = captured.block == null || pattern.matchesDirectionalPredicate(cell.predicate, scratch, front, up, flipped)
				PreparedCell(cell.path, cell.repeatPath, cell.depth, cell.height, cell.width, captured.position.asLong(), cell.token, cell.scopes.toTypedArray(), root, captured.block != null, directional, shareable)
			}
			val requirementList = ObjectArrayList<PreparedRequirement>()
			for (declaredConstraint in definition.constraints) {
				if (declaredConstraint !is PatternConstraint.Count) continue
				val scope = when (val declared = declaredConstraint.scope) {
					is PatternConstraint.Scope.All -> "all"
					is PatternConstraint.Scope.Node -> "node:${declared.id}"
					is PatternConstraint.Scope.Repeat -> "repeat:${declared.id}"
					is PatternConstraint.Scope.Fragment -> "fragment:${declared.id}"
				}
				requirementList.add(PreparedRequirement(declaredConstraint.fact, scope, declaredConstraint.minimum, declaredConstraint.maximum, declaredConstraint.message))
			}
			val requirements = requirementList.toTypedArray()
			val scopes = Array(layout.repeats.size) { index ->
				val instance = layout.repeats[index]
				PreparedScope("repeat:${instance.descriptor.id}@${instance.path}", -1, instance.depth, instance.height, instance.width, position(instance.width, instance.height, instance.depth).asLong(), instance.path)
			}
			val groups = PreparedDependencyGroups.build(cells, Array(captures.size) { captures[it].dependencies }, requirements, affected, reusable?.confirmed)
			val orientationValid = !((flipped && !definition.orientation.allowMirror) || (front.axis == Direction.Axis.Y && !definition.orientation.allowExtendedFacing))
			val input = PreparedPatternInput(cells, compiler.counterDefinitions(), requirements, scopes, orientationValid, center.asLong(), groups)
			return PatternPreparation(pattern, layout, target, center.immutable(), front, up, flipped, save, repetitions.copyOf(), selections, captures, blocks, compiler, flags, input)
		}

		internal fun encodeIO(io: IO): Int = when (io) {
			IO.IN -> 1
			IO.OUT -> 2
			IO.NONE -> 3
			IO.BOTH -> 0
		}
		internal fun decodeIO(io: Int): IO? = when (io) {
			1 -> IO.IN
			2 -> IO.OUT
			3 -> IO.NONE
			0 -> IO.BOTH
			else -> null
		}

		private val READ_ORDER = LongComparator { left, right ->
			var order = (BlockPos.getX(left) shr 4).compareTo(BlockPos.getX(right) shr 4)
			if (order == 0) order = (BlockPos.getZ(left) shr 4).compareTo(BlockPos.getZ(right) shr 4)
			if (order == 0) order = (BlockPos.getY(left) shr 4).compareTo(BlockPos.getY(right) shr 4)
			if (order == 0) order = BlockPos.getY(left).compareTo(BlockPos.getY(right))
			if (order == 0) order = BlockPos.getZ(left).compareTo(BlockPos.getZ(right))
			if (order == 0) order = BlockPos.getX(left).compareTo(BlockPos.getX(right))
			order
		}
	}
}

internal data class CapturedPatternBlock(val block: BlockState?, val entity: BlockEntity?)
internal data class CapturedPatternCell(val position: BlockPos, val block: BlockState?, val entity: BlockEntity?, val part: IMultiPart?, val dependencies: LongArray)
