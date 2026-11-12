package com.gregtechceu.gtceu.api.multiblock.pattern.match

import com.gregtechceu.gtceu.api.block.ActiveBlock
import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.api.multiblock.pattern.layout.PatternLayout
import com.gregtechceu.gtceu.api.multiblock.pattern.layout.PatternLayoutCell
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.PatternObservation

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.longs.LongSet
import it.unimi.dsi.fastutil.objects.ObjectArrayList

/** Frozen observations for one controller's selected layout; mutable aggregation objects stay in its state. */
class PatternMatchCache internal constructor(
	val pattern: MultiBlockPattern,
	internal val layout: PatternLayout,
	internal val center: BlockPos,
	internal val front: Direction,
	internal val up: Direction,
	internal val flipped: Boolean,
	internal val repetitions: IntArray,
	internal val selections: Map<String, String>,
) {
	internal data class Position(val cell: PatternLayoutCell, val pos: BlockPos, val block: BlockState, val entity: BlockEntity?, val observation: PatternObservation, val dependencies: LongArray)
	internal val positions = ObjectArrayList<Position>()
	private val byDependency = Long2IntOpenHashMap().also { it.defaultReturnValue(-1) }
	private val overlaps = Long2ObjectOpenHashMap<IntArrayList>()
	private val dependencyScratch = LongOpenHashSet(2)
	var eventDriven = true
		private set

	internal fun add(state: MultiblockState, cell: PatternLayoutCell, observation: PatternObservation) {
		dependencyScratch.clear()
		cell.predicate.collectDependencies(state, dependencyScratch)
		val dependencies = dependencyScratch.toLongArray()
		for (dependency in dependencies) {
			val pos = BlockPos.of(dependency)
			state.recordRead(pos)
			state.addPosCache(pos)
			val previous = byDependency.putIfAbsent(dependency, positions.size)
			if (previous >= 0) overlaps.computeIfAbsent(dependency) { IntArrayList(2).also { it.add(previous) } }.add(positions.size)
		}
		positions.add(Position(cell, state.pos, state.blockState, state.blockEntity, observation, dependencies))
		eventDriven = eventDriven && cell.predicate.hasPositionDependencies()
	}

	internal fun unchanged(position: Position, changed: LongSet): Boolean {
		for (dependency in position.dependencies) if (changed.contains(dependency)) return false
		return true
	}

	/** Equivalent position contributions leave all aggregate counts and metadata unchanged. */
	internal suspend fun checkLocal(work: PatternWork<*>, state: MultiblockState, changed: LongSet): Boolean {
		val indices = IntOpenHashSet(changed.size)
		val changes = changed.iterator()
		while (changes.hasNext()) {
			work.checkpoint()
			val dependency = changes.nextLong()
			val affected = byDependency.get(dependency)
			if (affected >= 0) indices.add(affected)
			overlaps[dependency]?.let { indices.addAll(it) }
		}
		val affected = indices.iterator()
		while (affected.hasNext()) {
			work.checkpoint()
			val position = positions[affected.nextInt()]
			val predicate = position.cell.predicate
			if (position.entity != null) return false
			if (!state.update(position.pos, predicate) || state.blockState.hasBlockEntity()) return false
			if ((state.blockState.block is ActiveBlock) != (position.block.block is ActiveBlock)) return false
			if (!predicate.matchesObservation(state, position.observation) || !pattern.matchesDirectionalPredicate(predicate, state, front, up, flipped)) return false
			dependencyScratch.clear()
			predicate.collectDependencies(state, dependencyScratch)
			if (dependencyScratch.size != position.dependencies.size) return false
			for (dependency in position.dependencies) if (!dependencyScratch.contains(dependency)) return false
		}
		return true
	}
}
