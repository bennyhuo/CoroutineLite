package com.bennyhuo.kotlin.coroutines

import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

interface CoroutineScope {
    val coroutineContext: CoroutineContext
}

internal class ContextScope(context: CoroutineContext) : CoroutineScope {
    override val coroutineContext: CoroutineContext = context
}

operator fun CoroutineScope.plus(context: CoroutineContext): CoroutineScope =
        ContextScope(coroutineContext + context)

fun CoroutineScope.cancel() {
    val job = coroutineContext[Job]
            ?: error("Scope cannot be cancelled because it does not have a job: $this")
    job.cancel()
}

suspend fun <R> coroutineScope(block: suspend CoroutineScope.() -> R): R =
        suspendCancellableCoroutine { continuation ->
            val coroutine = ScopeCoroutine(continuation.context, continuation)
            block.startCoroutine(coroutine, coroutine)
        }

internal open class ScopeCoroutine<T>(
        context: CoroutineContext,
        protected val continuation: Continuation<T>
) : AbstractCoroutine<T>(context) {

    override fun resumeWith(result: Result<T>) {
        super.resumeWith(result)
        continuation.resumeWith(result)
    }
}


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

object GlobalScope : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = EmptyCoroutineContext
}