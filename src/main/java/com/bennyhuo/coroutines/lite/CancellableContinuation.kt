package com.bennyhuo.coroutines.lite

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.intercepted
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn

sealed class CancelState {
    object InComplete : CancelState()
    class Complete<T>(val value: T? = null, val exception: Throwable? = null) : CancelState()
    object Cancelled : CancelState()
}

class CancellableContinuation<T>(private val continuation: Continuation<T>) : Continuation<T> by continuation {

    private val state = AtomicReference<CancelState>(CancelState.InComplete)

    val isCompleted: Boolean
        get() = state.get() != CancelState.InComplete

    private val cancelHandlers = CopyOnWriteArrayList<CancelHandler>()

    override fun resumeWith(result: Result<T>) {
        state.updateAndGet { prev ->
            when (prev) {
                CancelState.InComplete -> {
                    CancelState.Complete(result.getOrNull(), result.exceptionOrNull())
                }
                CancelState.Cancelled -> {
                    prev
                }
                is CancelState.Complete<*> -> {
                    throw IllegalStateException("Already completed.")
                }
            }
        }
    }

    fun getResult(): Any? {
        return when (val currentState = state.get()) {
            CancelState.InComplete -> COROUTINE_SUSPENDED
            CancelState.Cancelled -> throw CancellationException("Continuation is cancelled.")
            else -> {
                (currentState as CancelState.Complete<T>).let {
                    it.exception?.let { throw it } ?: it.value
                }
            }
        }
    }

    private fun installCancelHandler() {
        if (isCompleted) return
        val parent = continuation.context[Job] ?: return
        parent.invokeOnCancel {
            doCancel()
        }
    }

    fun cancel() {
        if (isCompleted) return
        val parent = continuation.context[Job] ?: return
        parent.cancel()
    }

    fun invokeOnCancel(cancelHandler: CancelHandler) {
        cancelHandlers += cancelHandler
    }

    private fun doCancel() {
        state.updateAndGet { prev ->
            when(prev){
                CancelState.InComplete -> {
                    cancelHandlers.forEach(CancelHandler::invoke)
                    CancelState.Cancelled
                }
                CancelState.Cancelled,
                is CancelState.Complete<*> -> {
                    prev
                }
            }
        }
    }
}


suspend inline fun <T> suspendCancellableCoroutine(
        crossinline block: (CancellableContinuation<T>) -> Unit
): T = suspendCoroutineUninterceptedOrReturn { continuation ->
    val cancellable = CancellableContinuation(continuation.intercepted())
    block(cancellable)
    cancellable.getResult()
}