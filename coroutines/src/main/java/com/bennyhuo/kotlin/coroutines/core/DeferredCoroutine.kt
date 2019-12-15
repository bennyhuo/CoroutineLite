package com.bennyhuo.kotlin.coroutines.core

import com.bennyhuo.kotlin.coroutines.Deferred
import com.bennyhuo.kotlin.coroutines.cancel.suspendCancellableCoroutine
import java.util.concurrent.CancellationException
import kotlin.coroutines.CoroutineContext

class DeferredCoroutine<T>(context: CoroutineContext) : AbstractCoroutine<T>(context), Deferred<T> {

    override suspend fun await(): T {
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
        val disposable = doOnCompleted { result ->
            continuation.resumeWith(result)
        }
        continuation.invokeOnCancel { disposable.dispose() }
    }
}