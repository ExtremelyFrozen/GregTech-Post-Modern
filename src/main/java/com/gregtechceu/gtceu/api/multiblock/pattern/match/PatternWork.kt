package com.gregtechceu.gtceu.api.multiblock.pattern.match

import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.createCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/** A server-thread continuation; one advance performs at most one bounded batch of pattern work. */
internal class PatternWork<T>(action: suspend PatternWork<T>.() -> T) {

	private var next: Continuation<Unit>? = null
	private var completed: Result<T>? = null
	private var remaining = 32
	private var deadline = Long.MAX_VALUE

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

	fun advance(deadline: Long = Long.MAX_VALUE): Boolean {
		this.deadline = deadline
		next?.resume(Unit)
		return completed != null
	}

	suspend fun checkpoint() {
		if (--remaining != 0 && (deadline == Long.MAX_VALUE || System.nanoTime() < deadline)) return
		remaining = 32
		suspendCoroutine { next = it }
	}

	fun result(): T = completed!!.getOrThrow()

	fun finish(): T {
		while (!advance()) {}
		return result()
	}
}
