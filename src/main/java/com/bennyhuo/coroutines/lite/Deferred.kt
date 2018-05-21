package com.bennyhuo.coroutines.lite

import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.suspendCoroutine

/**
 * Created by benny on 2018/5/20.
 */
class Deferred<T>(context: CoroutineContext, block: suspend () -> T) : AbstractCoroutine<T>(context, block) {

    suspend fun await(): T {
        val currentState = state.get()
        return when (currentState) {
            is State.InComplete -> awaitSuspend()
            is State.Complete<*> -> (currentState.value as T?) ?: throw currentState.exception!!
            else -> throw IllegalStateException("Invalid State: $currentState")
        }
    }

    private suspend fun awaitSuspend() = suspendCoroutine<T> { continuation ->
        doOnCompleted { t, throwable ->
            when {
                t != null -> continuation.resume(t)
                throwable != null -> continuation.resumeWithException(throwable)
                else -> throw IllegalStateException("Won't happen.")
            }
        }
    }
}