package com.bennyhuo.coroutines.lite

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Created by benny on 2018/5/20.
 */
class Deferred<T>(context: CoroutineContext) : AbstractCoroutine<T>(context) {

    suspend fun await(): T {
        val currentState = state.get()
        return when (currentState) {
            is CoroutineState.InComplete,
            is CoroutineState.Cancelling -> awaitSuspend()
            is CoroutineState.Complete<*> -> (currentState.value as T?)
                    ?: throw currentState.exception!!
        }
    }

    private suspend fun awaitSuspend() = suspendCancellableCoroutine<T> { continuation ->
        doOnCompleted { t, throwable ->
            when {
                t != null -> continuation.resume(t)
                throwable != null -> continuation.resumeWithException(throwable)
                else -> throw IllegalStateException("Won't happen.")
            }
        }
    }
}