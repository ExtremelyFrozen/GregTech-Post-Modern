package com.gregtechceu.gtceu.api.multiblock.pattern.match

import com.gregtechceu.gtceu.api.multiblock.pattern.preparation.PreparedPatternCalculation
import com.gregtechceu.gtceu.api.multiblock.pattern.preparation.PreparedPatternInput
import com.gregtechceu.gtceu.api.multiblock.pattern.preparation.PreparedPatternResult

import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.createCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/** Only detached rule inputs leave the server thread; continuations always resume during the next server drain. */
internal class PatternWork<T>(action: suspend PatternWork<T>.() -> T) {

	private var next: Continuation<Unit>? = null
	private var completed: Result<T>? = null
	private var executor: Executor? = null
	private var pending: CompletableFuture<PreparedPatternResult>? = null
	private var pendingInput: PreparedPatternInput? = null
	private var resumeComputed: (() -> Unit)? = null
	private var cancelled = false

	init {
		next = action.createCoroutine(
			this,
			object : Continuation<T> {
				override val context = EmptyCoroutineContext
				override fun resumeWith(result: Result<T>) {
					completed = result
					next = null
				}
			},
		)
	}

	fun advance(executor: Executor? = null): Boolean {
		this.executor = executor
		val computation = pending
		if (computation != null) {
			if (!computation.isDone) return false
			pending = null
			pendingInput = null
			val resume = resumeComputed!!
			resumeComputed = null
			resume()
		} else {
			next?.also { next = null }?.resume(Unit)
		}
		return completed != null
	}

	suspend fun checkpoint() {
		if (cancelled) throw CancellationException("Structure check was cancelled")
	}

	suspend fun compute(input: PreparedPatternInput): PreparedPatternResult {
		val lane = executor ?: return input.evaluate()
		val computation = PreparedPatternCalculation.submit(input, lane)
		pending = computation
		pendingInput = input
		return suspendCoroutine { continuation ->
			resumeComputed = { continuation.resumeWith(runCatching { computation.join() }) }
		}
	}

	fun cancel() {
		cancelled = true
		pendingInput?.cancel()
		pendingInput = null
		pending?.cancel(false)
		pending = null
		resumeComputed = null
		next = null
	}

	fun result(): T = completed!!.getOrThrow()

	fun finish(): T {
		while (!advance()) {}
		return result()
	}
}
