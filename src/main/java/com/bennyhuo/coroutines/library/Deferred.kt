package com.bennyhuo.coroutines.library

import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.suspendCoroutine

/**
 * Created by benny on 2018/5/20.
 */
class Deferred<T>(context: CoroutineContext, block: suspend () -> T): AbstractCoroutine<T>(context, block) {

    suspend fun await(): T {
        val currentState = state.get()
        when (currentState) {
            is State.InComplete -> {
                return suspendCoroutine { continuation ->
                    if (!state.compareAndSet(State.InComplete,
                                    State.CompleteHandler<T> { t, throwable ->
                                        when {
                                            t != null -> continuation.resume(t)
                                            throwable != null -> continuation.resumeWithException(throwable)
                                            else -> continuation.resumeWithException(IllegalStateException("Cannot happen."))
                                        }
                                    })
                    ) {
                        val currentState = state.get()
                        when (currentState) {
                            is State.Complete<*> -> {
                                (currentState as State.Complete<T>).let {
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
            is State.Complete<*> -> {
                return (currentState.value as T?) ?: throw currentState.exception!!
            }
            else -> {
                throw IllegalStateException("Invalid State: $currentState")
            }
        }
    }
}