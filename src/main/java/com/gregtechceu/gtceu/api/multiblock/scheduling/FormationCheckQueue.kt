package com.gregtechceu.gtceu.api.multiblock.scheduling

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectList
import it.unimi.dsi.fastutil.objects.ObjectLists
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet

/** Registered structure checks stay in one list; newer manual checks move ahead of older work. */
class FormationCheckQueue<T : FormationCheckQueue.Entry> {

	private val entries = ReferenceLinkedOpenHashSet<T>()
	private var firstDueTick = Long.MAX_VALUE

	fun register(entry: T, dueTick: Long) {
		entry.dueTick = dueTick
		entries.add(entry)
		firstDueTick = minOf(firstDueTick, dueTick)
	}

	fun registerFirst(entry: T, dueTick: Long) {
		entry.dueTick = dueTick
		entries.addAndMoveToFirst(entry)
		firstDueTick = minOf(firstDueTick, dueTick)
	}

	fun remove(entry: T) {
		entries.remove(entry)
	}

	fun pollDue(tick: Long): ObjectList<T> {
		if (tick < firstDueTick) return ObjectLists.emptyList()
		val due = ObjectArrayList<T>()
		val iterator = entries.iterator()
		firstDueTick = Long.MAX_VALUE
		while (iterator.hasNext()) {
			val entry = iterator.next()
			if (entry.dueTick <= tick) {
				due.add(entry)
				iterator.remove()
			} else {
				firstDueTick = minOf(firstDueTick, entry.dueTick)
			}
		}
		return due
	}

	fun clear() {
		entries.clear()
		firstDueTick = Long.MAX_VALUE
	}

	abstract class Entry {
		internal var dueTick = 0L
		fun dueTick(): Long = dueTick
	}
}
