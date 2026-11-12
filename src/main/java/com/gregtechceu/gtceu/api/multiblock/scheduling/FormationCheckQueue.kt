package com.gregtechceu.gtceu.api.multiblock.scheduling

import it.unimi.dsi.fastutil.objects.ObjectArrayList

/** Server-thread queue with cancellable entries; yielded checks rotate behind peers at the same priority. */
class FormationCheckQueue<T : FormationCheckQueue.Entry> {

	private val changes = ObjectArrayList<T>()
	private val searches = ObjectArrayList<T>()
	private var order = 0L

	/** Schedules an owned entry, replacing its previous slot without retaining cancelled sessions. */
	fun schedule(entry: T, dueTick: Long, priority: Boolean) {
		remove(entry)
		entry.dueTick = dueTick
		entry.order = order++
		entry.priority = priority
		val heap = if (priority) changes else searches
		entry.index = heap.size
		heap.add(entry)
		siftUp(heap, entry.index)
	}

	fun hasDue(tick: Long): Boolean = isDue(changes, tick) || isDue(searches, tick)

	/** Returns a due change check before a first-formation search, or null when both queues are idle. */
	fun poll(tick: Long): T? {
		val heap = if (isDue(changes, tick)) changes else searches
		if (!isDue(heap, tick)) return null
		return heap.first().also(::remove)
	}

	fun remove(entry: T) {
		val index = entry.index
		if (index < 0) return
		val heap = if (entry.priority) changes else searches
		val last = heap.removeLast()
		entry.index = -1
		if (index == heap.size) return
		heap[index] = last
		last.index = index
		if (index > 0 && before(last, heap[(index - 1) ushr 1])) siftUp(heap, index) else siftDown(heap, index)
	}

	fun clear() {
		for (entry in changes) entry.index = -1
		for (entry in searches) entry.index = -1
		changes.clear()
		searches.clear()
	}

	private fun isDue(heap: ObjectArrayList<T>, tick: Long): Boolean = heap.isNotEmpty() && heap.first().dueTick <= tick
	private fun before(left: Entry, right: Entry): Boolean = left.dueTick < right.dueTick || (left.dueTick == right.dueTick && left.order < right.order)

	private fun siftUp(heap: ObjectArrayList<T>, start: Int) {
		var index = start
		val entry = heap[index]
		while (index > 0) {
			val parent = (index - 1) ushr 1
			val previous = heap[parent]
			if (!before(entry, previous)) break
			heap[index] = previous
			previous.index = index
			index = parent
		}
		heap[index] = entry
		entry.index = index
	}

	private fun siftDown(heap: ObjectArrayList<T>, start: Int) {
		var index = start
		val entry = heap[index]
		val half = heap.size ushr 1
		while (index < half) {
			var child = index * 2 + 1
			if (child + 1 < heap.size && before(heap[child + 1], heap[child])) child++
			val next = heap[child]
			if (!before(next, entry)) break
			heap[index] = next
			next.index = index
			index = child
		}
		heap[index] = entry
		entry.index = index
	}

	/** One entry belongs to one queue; only schedule/remove may change its heap ordering. */
	abstract class Entry {
		internal var dueTick = 0L
		internal var order = 0L
		internal var priority = false
		internal var index = -1
		fun dueTick(): Long = dueTick
	}
}
