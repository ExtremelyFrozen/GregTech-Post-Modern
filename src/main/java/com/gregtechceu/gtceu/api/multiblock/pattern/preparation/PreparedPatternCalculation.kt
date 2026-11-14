package com.gregtechceu.gtceu.api.multiblock.pattern.preparation

import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

/** Schedules detached rule programs only; each independent group yields its queue slot before the next one. */
object PreparedPatternCalculation {
	@JvmStatic
	fun submit(input: PreparedPatternInput, executor: Executor): CompletableFuture<PreparedPatternResult> {
		val future = CalculationFuture(input)
		Calculation(input, executor, future).enqueue()
		return future
	}

	private class CalculationFuture(private val input: PreparedPatternInput) : CompletableFuture<PreparedPatternResult>() {
		override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
			val changed = super.cancel(false)
			if (changed) input.cancel()
			return changed
		}
	}

	private class Calculation(private val input: PreparedPatternInput, private val executor: Executor, private val future: CompletableFuture<PreparedPatternResult>) : Runnable {
		private val groups = input.groups?.let { PreparedGroupEvaluator(input, it) }
		private val running = AtomicBoolean()
		private val inlineContinuation = AtomicBoolean()

		fun enqueue() {
			if (future.isDone) return
			try {
				executor.execute(this)
			} catch (exception: RuntimeException) {
				future.completeExceptionally(exception)
			}
		}

		override fun run() {
			// Direct executors may invoke the next queue slot inline. Trampoline those invocations without recursion.
			if (!running.compareAndSet(false, true)) {
				inlineContinuation.set(true)
				return
			}
			try {
				do {
					inlineContinuation.set(false)
					if (future.isDone) return
					input.checkpoint()
					val result = if (groups == null) input.evaluate() else groups.advance()
					if (future.isDone) return
					if (result != null) {
						future.complete(result)
						return
					}
					enqueue()
				} while (inlineContinuation.get())
			} catch (exception: CancellationException) {
				future.cancel(false)
			} catch (exception: Throwable) {
				future.completeExceptionally(exception)
			} finally {
				running.set(false)
				// A pool may claim the requeued slot before this invocation releases the cursor.
				if (inlineContinuation.getAndSet(false) && !future.isDone) enqueue()
			}
		}
	}
}
