package com.bennyhuo.kotlin.coroutines

import com.bennyhuo.kotlin.coroutines.core.AbstractCoroutine
import com.bennyhuo.kotlin.coroutines.core.CoroutineState
import com.bennyhuo.kotlin.coroutines.cancel.suspendCancellableCoroutine
import kotlin.coroutines.CoroutineContext

/**
 * Created by benny on 2018/5/20.
 */
class Deferred<T>(context: CoroutineContext) : AbstractCoroutine<T>(context) {

    suspend fun await(): T {
        val currentState = state.get()
        return when (currentState) {
            is CoroutineState.InComplete,
            is CoroutineState.Cancelling -> awaitSuspend()
            is CoroutineState.Complete<*> -> {
                if(parentJob != null && !parentJob.isActive){
                    throw CancellationException("Parent cancelled.")
                }
                (currentState.value as T?)
                        ?: throw currentState.exception!!
            }
        }
    }

    private suspend fun awaitSuspend() = suspendCancellableCoroutine<T> { continuation ->
        doOnCompleted { result ->
            continuation.resumeWith(result)
        }
    }
}