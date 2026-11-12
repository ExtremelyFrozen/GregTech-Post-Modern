package com.gregtechceu.gtceu.api.multiblock.pattern.match

import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.api.multiblock.error.PatternStringError
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternDirection

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction

import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2IntMap
import it.unimi.dsi.fastutil.objects.Object2IntMaps
import it.unimi.dsi.fastutil.objects.ObjectLists

/** Boundary probes use the same explicit dependency tracking as ordinary layout positions. */
internal object PatternMeasurements {
	fun read(pattern: MultiBlockPattern, state: MultiblockState, center: BlockPos, front: Direction, up: Direction, flipped: Boolean, probe: (() -> Boolean)? = null): Object2IntMap<String>? {
		val measurements = pattern.getMeasurements()
		if (measurements.isEmpty()) return Object2IntMaps.emptyMap()
		val values = Object2IntLinkedOpenHashMap<String>()
		val dependencies = LongOpenHashSet()
		fun failure(id: String, position: BlockPos, depth: Int, height: Int, width: Int, expected: String): Object2IntMap<String>? {
			val diagnostic = PatternDiagnostic("$/measurement:$id", "", PatternCoordinate(depth, height, width), position, if (state.hasReadPosition(position.asLong()) && state.world.isLoaded(position)) state.world.getBlockState(position) else null, expected, "measurement:$id", 1)
			state.matchResult = PatternMatchResult(false, state.facts, ObjectLists.singleton(diagnostic))
			state.setError(PatternStringError("gtpm.multiblock.pattern.error.constraint"))
			return null
		}
		var index = 0
		for ((id, parameter, start, direction, minimum, maximum) in measurements) {
			val predicate = pattern.measurementRule(index++)
			val sign = if (direction.ordinal % 2 == 0) 1 else -1
			var found = -1
			for (distance in (if (minimum == 0) 0 else 1)..maximum) {
				val depth = start.depth + if (direction == PatternDirection.FRONT || direction == PatternDirection.BACK) sign * distance else 0
				val height = start.height + if (direction == PatternDirection.UP || direction == PatternDirection.DOWN) sign * distance else 0
				val width = start.width + if (direction == PatternDirection.LEFT || direction == PatternDirection.RIGHT) sign * distance else 0
				val position = center.offset(pattern.getActualRelativeOffset(width, height, depth, front, up, flipped))
				if (probe?.invoke() == false) return failure(id, position, depth, height, width, "measurement work limit")
				state.addPosCache(position)
				if (!state.update(position, predicate)) return failure(id, position, depth, height, width, "loaded measurement position")
				dependencies.clear()
				predicate.collectDependencies(state, dependencies)
				val positions = dependencies.iterator()
				while (positions.hasNext()) state.recordRead(BlockPos.of(positions.nextLong()))
				val observed = predicate.observePosition(state)
				if (observed.matched) {
					found = distance
					break
				}
			}
			if (found < minimum) {
				val distance = if (found >= 0) found else maximum
				val depth = start.depth + if (direction == PatternDirection.FRONT || direction == PatternDirection.BACK) sign * distance else 0
				val height = start.height + if (direction == PatternDirection.UP || direction == PatternDirection.DOWN) sign * distance else 0
				val width = start.width + if (direction == PatternDirection.LEFT || direction == PatternDirection.RIGHT) sign * distance else 0
				val position = center.offset(pattern.getActualRelativeOffset(width, height, depth, front, up, flipped))
				return failure(id, position, depth, height, width, if (found < 0) "measurement boundary within $maximum" else "measurement minimum $minimum")
			}
			values[parameter] = found
		}
		return values
	}
}
