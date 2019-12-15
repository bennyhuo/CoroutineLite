package com.bennyhuo.kotlin.coroutines.cancel

import com.bennyhuo.kotlin.coroutines.CancellationException
import com.bennyhuo.kotlin.coroutines.Job
import com.bennyhuo.kotlin.coroutines.OnCancel
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.intercepted
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn

class CancellableContinuation<T>(private val continuation: Continuation<T>) : Continuation<T> by continuation {

    private val state = AtomicReference<CancelState>(CancelState.InComplete)

    val isCompleted: Boolean
        get() = state.get() != CancelState.InComplete

    private val cancelHandlers = CopyOnWriteArrayList<OnCancel>()

    override fun resumeWith(result: Result<T>) {
        state.updateAndGet { prev ->
            when (prev) {
                CancelState.InComplete -> {
                    continuation.resumeWith(result)
                    CancelState.Complete(result.getOrNull(), result.exceptionOrNull())
                }
                CancelState.Cancelled -> {
                    CancellationException("Cancelled.").let {
                        continuation.resumeWith(Result.failure(it))
                        CancelState.Complete(null, it)
                    }
                }
                is CancelState.Complete<*> -> {
                    throw IllegalStateException("Already completed.")
                }
            }
        }
    }

    fun getResult(): Any? {
        installCancelHandler()
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

    fun invokeOnCancel(onCancel: OnCancel) {
        cancelHandlers += onCancel
    }

    private fun doCancel() {
        state.updateAndGet { prev ->
            when (prev) {
                CancelState.InComplete -> {
                    CancelState.Cancelled
                }
                CancelState.Cancelled,
                is CancelState.Complete<*> -> {
                    prev
                }
            }
        }
        cancelHandlers.forEach(OnCancel::invoke)
        cancelHandlers.clear()
    }
}


suspend inline fun <T> suspendCancellableCoroutine(
        crossinline block: (CancellableContinuation<T>) -> Unit
): T = suspendCoroutineUninterceptedOrReturn { continuation ->
    val cancellable = CancellableContinuation(continuation.intercepted())
    block(cancellable)
    cancellable.getResult()
}