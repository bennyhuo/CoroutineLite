package com.bennyhuo.kotlin.coroutines.scope

import com.bennyhuo.kotlin.coroutines.cancel.suspendCancellableCoroutine
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.startCoroutine

private class SupervisorCoroutine<T>(
        context: CoroutineContext,
        continuation: Continuation<T>
) : ScopeCoroutine<T>(context, continuation) {

    override fun handleChildException(e: Throwable): Boolean {
        return false
    }

}

suspend fun <R> supervisorScope(block: suspend CoroutineScope.() -> R): R =
        suspendCancellableCoroutine { continuation ->
            val coroutine = SupervisorCoroutine(continuation.context, continuation)
            block.startCoroutine(coroutine, coroutine)
        }