package com.gregtechceu.gtceu.api.multiblock.scheduling

import java.util.concurrent.Executor
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/** One server's detached calculations, ordered before execution and rotated at candidate boundaries. */
class FormationLane internal constructor(name: String) : AutoCloseable {

	private class Calculation(val order: Long, val action: Runnable) :
		Runnable,
		Comparable<Calculation> {
		override fun compareTo(other: Calculation): Int = order.compareTo(other.order)
		override fun run() = action.run()
	}

	private val order = AtomicLong()
	private val executor = ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, PriorityBlockingQueue()) { action ->
		Thread(action, name).also { it.isDaemon = true }
	}
	private val regular = Executor { action -> executor.execute(Calculation(order.getAndIncrement(), action)) }

	fun executor(): Executor = regular

	fun firstExecutor(): Executor {
		val first = AtomicBoolean(true)
		val firstOrder = -order.getAndIncrement() - 1
		return Executor { action ->
			val position = if (first.getAndSet(false)) firstOrder else order.getAndIncrement()
			executor.execute(Calculation(position, action))
		}
	}

	override fun close() {
		executor.shutdownNow()
	}
}
