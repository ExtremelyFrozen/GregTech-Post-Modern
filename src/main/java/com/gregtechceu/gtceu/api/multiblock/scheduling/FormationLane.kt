package com.gregtechceu.gtceu.api.multiblock.scheduling

import java.util.concurrent.Executor
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/** One server's detached calculations, ordered before execution and rotated at candidate boundaries. */
class FormationLane internal constructor(name: String) : AutoCloseable {
	enum class Priority { CHANGE, SEARCH, RECHECK }

	private class Calculation(val priority: Priority, val order: Long, val action: Runnable) :
		Runnable,
		Comparable<Calculation> {
		override fun compareTo(other: Calculation): Int {
			val priorityOrder = priority.ordinal.compareTo(other.priority.ordinal)
			return if (priorityOrder == 0) order.compareTo(other.order) else priorityOrder
		}
		override fun run() = action.run()
	}

	private val order = AtomicLong()
	private val executor = ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, PriorityBlockingQueue()) { action ->
		Thread(action, name).also { it.isDaemon = true }
	}
	private val lanes = Array(Priority.entries.size) { index -> Executor { action -> executor.execute(Calculation(Priority.entries[index], order.getAndIncrement(), action)) } }

	fun executor(priority: Priority): Executor = lanes[priority.ordinal]

	override fun close() {
		executor.shutdownNow()
	}
}
