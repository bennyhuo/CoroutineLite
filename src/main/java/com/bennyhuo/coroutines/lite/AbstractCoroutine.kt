package com.bennyhuo.coroutines.lite

import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.startCoroutine
import kotlin.coroutines.experimental.suspendCoroutine

typealias OnComplete<T> = (T?, Throwable?) -> Unit

sealed class State {
    object InComplete : State()
    class Complete<T>(val value: T? = null, val exception: Throwable? = null) : State()
    class CompleteHandler<T>(val handler: OnComplete<T>) : State()
}

abstract class AbstractCoroutine<T>(override val context: CoroutineContext, block: suspend () -> T) : Continuation<T> {

    protected val state = AtomicReference<State>()

    init {
        state.set(State.InComplete)
        block.startCoroutine(this)
    }

    val isCompleted
        get() = state.get() is State.Complete<*>

    override fun resume(value: T) {
        val currentState = state.getAndSet(State.Complete(value))
        when (currentState) {
            is State.CompleteHandler<*> -> {
                (currentState as State.CompleteHandler<T>).handler(value, null)
            }
        }
    }

    override fun resumeWithException(exception: Throwable) {
        val currentState = state.getAndSet(State.Complete<T>(null, exception))
        when (currentState) {
            is State.CompleteHandler<*> -> {
                (currentState as State.CompleteHandler<T>).handler(null, exception)
            }
        }
    }

    suspend fun join() {
        val currentState = state.get()
        when (currentState) {
            is State.InComplete -> return joinSuspend()
            is State.Complete<*> -> return
            else -> throw IllegalStateException("Invalid State: $currentState")
        }
    }

    private suspend fun joinSuspend() = suspendCoroutine<Unit> { continuation ->
        doOnCompleted { t, throwable -> continuation.resume(Unit) }
    }

    protected fun doOnCompleted(block: (T?, Throwable?) -> Unit) {
        if (!state.compareAndSet(State.InComplete, State.CompleteHandler<T>(block))) {
            val currentState = state.get()
            when (currentState) {
                is State.Complete<*> -> {
                    (currentState as State.Complete<T>).let {
                        block(currentState.value, currentState.exception)
                    }
                }
                else -> throw IllegalStateException("Invalid State: $currentState")
            }
        }
    }
}