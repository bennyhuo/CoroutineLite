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
        get() = when(state.get()){
            CancelState.InComplete,
            is CancelState.CancelHandler -> false
            is CancelState.Complete<*>,
            CancelState.Cancelled -> true
        }

    override fun resumeWith(result: Result<T>) {
        state.updateAndGet { prev ->
            when (prev) {
                is CancelState.CancelHandler,
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
            is CancelState.CancelHandler,
            CancelState.InComplete -> COROUTINE_SUSPENDED
            CancelState.Cancelled -> throw CancellationException("Continuation is cancelled.")
            is CancelState.Complete<*> -> {
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

    fun invokeOnCancellation(onCancel: OnCancel) {
        val newState = state.updateAndGet { prev ->
            when(prev){
                CancelState.InComplete -> CancelState.CancelHandler(onCancel)
                is CancelState.CancelHandler -> throw IllegalStateException("It's prohibited to register multiple handlers.")
                is CancelState.Complete<*>,
                CancelState.Cancelled -> prev
            }
        }
        if(newState is CancelState.Cancelled){
            onCancel()
        }
    }

    private fun doCancel() {
        val prevState = state.getAndUpdate { prev ->
            when (prev) {
                is CancelState.CancelHandler,
                    CancelState.InComplete -> {
                    CancelState.Cancelled
                }
                CancelState.Cancelled,
                is CancelState.Complete<*> -> {
                    prev
                }
            }
        }
        if(prevState is CancelState.CancelHandler){
            prevState.onCancel()
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