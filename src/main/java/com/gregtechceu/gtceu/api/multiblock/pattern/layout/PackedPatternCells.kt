package com.gregtechceu.gtceu.api.multiblock.pattern.layout

import com.gregtechceu.gtceu.api.multiblock.structurepredicate.PatternPredicate

import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.objects.AbstractObjectList
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectList
import it.unimi.dsi.fastutil.objects.ObjectLists
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap

import java.util.RandomAccess

/** Immutable primitive storage; cell records are materialized only when the list view is read. */
class PackedPatternCells internal constructor(source: ObjectList<PatternLayoutCell>) :
	AbstractObjectList<PatternLayoutCell>(),
	RandomAccess {
	override val size: Int = source.size
	private val minimum = IntArray(3)
	private val maximum = IntArray(3)
	private val dimensions = LongArray(3)
	private val coordinateBits: IntArray
	private val coordinates: IntArray?
	private val depths: IntArray?
	private val heights: IntArray?
	private val widths: IntArray?
	private val predicates: Array<PatternPredicate>
	private val bytePredicates: ByteArray?
	private val shortPredicates: ShortArray?
	private val intPredicates: IntArray?
	private val strings: Array<String>
	private val scopeLists: ObjectList<ObjectList<String>>
	private val pathIds = IntArray(size)
	private val repeatPathIds = IntArray(size)
	private val tokenIds = IntArray(size)
	private val scopeIds = IntArray(size)
	private val declarationPathIds = IntArray(size)
	private val units = IntArray(size)
	private val repetitions = IntArray(size)
	private val innerCoordinates = IntArray(size)
	private val rows = IntArray(size)
	private val columns = IntArray(size)
	private val layerEnds: IntArray

	val layerCount: Int get() = layerEnds.size

	init {
		if (isNotEmpty()) {
			minimum.fill(Int.MAX_VALUE)
			maximum.fill(Int.MIN_VALUE)
			for ((_, _, depth, height, width) in source) {
				minimum[0] = minOf(minimum[0], depth)
				minimum[1] = minOf(minimum[1], height)
				minimum[2] = minOf(minimum[2], width)
				maximum[0] = maxOf(maximum[0], depth)
				maximum[1] = maxOf(maximum[1], height)
				maximum[2] = maxOf(maximum[2], width)
			}
			for (axis in 0..2) dimensions[axis] = maximum[axis].toLong() - minimum[axis].toLong() + 1L
		}
		coordinateBits = IntArray(3) { axis ->
			val range = if (isEmpty) 0L else dimensions[axis] - 1L
			Long.SIZE_BITS - range.countLeadingZeroBits()
		}
		if (coordinateBits.sum() <= Int.SIZE_BITS) {
			coordinates = IntArray(size)
			depths = null
			heights = null
			widths = null
		} else {
			coordinates = null
			depths = IntArray(size)
			heights = IntArray(size)
			widths = IntArray(size)
		}

		val predicateIds = Reference2IntOpenHashMap<PatternPredicate>()
		predicateIds.defaultReturnValue(-1)
		val predicatePalette = ObjectArrayList<PatternPredicate>()
		val temporaryPredicates = IntArray(size)
		val stringIds = Object2IntOpenHashMap<String>()
		stringIds.defaultReturnValue(-1)
		val stringPalette = ObjectArrayList<String>()
		val scopes = Object2IntOpenHashMap<ObjectList<String>>()
		scopes.defaultReturnValue(-1)
		val scopePalette = ObjectArrayList<ObjectList<String>>()
		val ends = IntArrayList()

		fun intern(value: String): Int {
			val existing = stringIds.getInt(value)
			if (existing >= 0) return existing
			val id = stringPalette.size
			stringIds.put(value, id)
			stringPalette.add(value)
			return id
		}

		for (index in indices) {
			val cell = source[index]
			if (coordinates != null) {
				val depth = cell.depth.toLong() - minimum[0].toLong()
				val height = cell.height.toLong() - minimum[1].toLong()
				val width = cell.width.toLong() - minimum[2].toLong()
				coordinates[index] = (depth or (height shl coordinateBits[0]) or (width shl (coordinateBits[0] + coordinateBits[1]))).toInt()
			} else {
				depths!![index] = cell.depth
				heights!![index] = cell.height
				widths!![index] = cell.width
			}
			var predicateId = predicateIds.getInt(cell.predicate)
			if (predicateId < 0) {
				predicateId = predicatePalette.size
				predicateIds.put(cell.predicate, predicateId)
				predicatePalette.add(cell.predicate)
			}
			temporaryPredicates[index] = predicateId
			pathIds[index] = intern(cell.path)
			repeatPathIds[index] = intern(cell.repeatPath)
			tokenIds[index] = intern(cell.token)
			declarationPathIds[index] = intern(cell.declarationPath)
			var scopeId = scopes.getInt(cell.scopes)
			if (scopeId < 0) {
				scopeId = scopePalette.size
				val canonical = ObjectArrayList<String>(cell.scopes.size)
				for (scope in cell.scopes) canonical.add(stringPalette[intern(scope)])
				val immutable = ObjectLists.unmodifiable(canonical)
				scopes.put(immutable, scopeId)
				scopePalette.add(immutable)
			}
			scopeIds[index] = scopeId
			units[index] = cell.unit
			repetitions[index] = cell.repetition
			innerCoordinates[index] = cell.inner
			rows[index] = cell.row
			columns[index] = cell.column
			if (index + 1 == size || source[index + 1].depth != cell.depth) ends.add(index + 1)
		}
		predicates = predicatePalette.toTypedArray()
		when {
			predicates.size <= 256 -> {
				bytePredicates = ByteArray(size) { temporaryPredicates[it].toByte() }
				shortPredicates = null
				intPredicates = null
			}

			predicates.size <= 65536 -> {
				bytePredicates = null
				shortPredicates = ShortArray(size) { temporaryPredicates[it].toShort() }
				intPredicates = null
			}

			else -> {
				bytePredicates = null
				shortPredicates = null
				intPredicates = temporaryPredicates
			}
		}
		strings = stringPalette.toTypedArray()
		scopeLists = ObjectLists.unmodifiable(scopePalette)
		layerEnds = ends.toIntArray()
	}

	override fun get(index: Int): PatternLayoutCell = PatternLayoutCell(
		strings[pathIds[index]], strings[repeatPathIds[index]],
		depth(
			index,
		),
		height(index), width(index), predicate(index), strings[tokenIds[index]], scopeLists[scopeIds[index]], units[index], repetitions[index], innerCoordinates[index], rows[index], columns[index], strings[declarationPathIds[index]],
	)

	fun depth(index: Int): Int = if (coordinates == null) depths!![index] else coordinate(index, 0, 0)

	fun height(index: Int): Int = if (coordinates == null) heights!![index] else coordinate(index, 1, coordinateBits[0])

	fun width(index: Int): Int = if (coordinates == null) widths!![index] else coordinate(index, 2, coordinateBits[0] + coordinateBits[1])

	fun predicate(index: Int): PatternPredicate {
		val id = when {
			bytePredicates != null -> bytePredicates[index].toInt() and 0xff
			shortPredicates != null -> shortPredicates[index].toInt() and 0xffff
			else -> intPredicates!![index]
		}
		return predicates[id]
	}

	fun minimum(axis: PatternAxis): Int = minimum[axis.ordinal]
	fun maximum(axis: PatternAxis): Int = maximum[axis.ordinal]
	fun size(axis: PatternAxis): Int = Math.toIntExact(dimensions[axis.ordinal])

	/** Start (inclusive) and end (exclusive) of a contiguous depth run, preserving source order. */
	fun layerStart(index: Int): Int = if (index == 0) 0 else layerEnds[index - 1]

	fun layerEnd(index: Int): Int = layerEnds[index]

	private fun coordinate(index: Int, axis: Int, shift: Int): Int {
		val mask = (1L shl coordinateBits[axis]) - 1L
		val normalized = (coordinates!![index].toLong() and 0xffffffffL) ushr shift and mask
		return (minimum[axis].toLong() + normalized).toInt()
	}
}
