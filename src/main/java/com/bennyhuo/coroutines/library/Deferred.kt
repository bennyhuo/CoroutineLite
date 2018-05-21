package com.bennyhuo.coroutines

import com.bennyhuo.coroutines.State.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.startCoroutine
import kotlin.coroutines.experimental.suspendCoroutine

/**
 * Created by benny on 2018/5/20.
 */
typealias OnComplete<T> = (T?, Throwable?) -> Unit

sealed class State {
    object InComplete : State()
    class Complete<T>(val value: T? = null, val exception: Exception? = null) : State()
    class CompleteHandler<T>(val handler: OnComplete<T>) : State()
}

class Deferred<T>(context: CoroutineContext, block: suspend () -> T) {

    private val state = AtomicReference<State>()

    init {
        state.set(InComplete)
        suspend {
            val complete = try {
                Complete(block())
            } catch (e: Exception) {
                Complete<T>(exception = e)
            }
            val currentState = state.getAndSet(complete)
            when (currentState) {
                is CompleteHandler<*> -> {
                    (currentState as CompleteHandler<T>).handler(complete.value, complete.exception)
                }
            }
        }.startCoroutine(ContextContinuation(context))
    }

    suspend fun await(): T {
        val currentState = state.get()
        when (currentState) {
            is InComplete -> {
                return suspendCoroutine { continuation ->
                    if (!state.compareAndSet(InComplete,
                                    CompleteHandler<T> { t, throwable ->
                                        if (t != null) {
                                            continuation.resume(t)
                                        } else if (throwable != null) {
                                            continuation.resumeWithException(throwable)
                                        } else {
                                            continuation.resumeWithException(IllegalStateException("Cannot happen."))
                                        }
                                    })
                    ) {
                        val currentState = state.get()
                        when (currentState) {
                            is Complete<*> -> {
                                (currentState as Complete<T>).let {
                                    currentState.value?.let(continuation::resume)
                                            ?: continuation.resumeWithException(currentState.exception!!)
                                }
                            }
                            else -> {
                                throw IllegalStateException("Invalid State: $currentState")
                            }
                        }
                    }
                }
            }
            is Complete<*> -> {
                return (currentState.value as T?) ?: throw currentState.exception!!
            }
            else -> {
                throw IllegalStateException("Invalid State: $currentState")
            }
        }
    }
}