package com.bennyhuo.coroutines.lite

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.resume

typealias OnComplete<T> = (T?, Throwable?) -> Unit

sealed class CoroutineState {
    object InComplete : CoroutineState()
    class Complete<T>(val value: T? = null, val exception: Throwable? = null) : CoroutineState()
    class CompleteHandler<T> : CoroutineState() {

        private val handlers = CopyOnWriteArrayList<OnComplete<T>>()

        fun invokeOnComplete(onComplete: OnComplete<T>) {
            handlers += onComplete
        }

        fun doOnCompleted(value: T?, throwable: Throwable?) {
            handlers.forEach {
                it.invoke(value, throwable)
            }
        }
    }
}

abstract class AbstractCoroutine<T>(override val context: CoroutineContext, block: suspend () -> T) : Job, Continuation<T> {

    protected val state = AtomicReference<CoroutineState>()

    init {
        state.set(CoroutineState.InComplete)
        block.startCoroutine(this)
    }

    val isCompleted
        get() = state.get() is CoroutineState.Complete<*>

    private val cancelHandlers = CopyOnWriteArrayList<CancelHandler>()

    override fun resumeWith(result: Result<T>) {
        val newState = state.updateAndGet { prev ->
            when (prev) {
                CoroutineState.InComplete -> {
                    CoroutineState.Complete(result.getOrNull(), result.exceptionOrNull())
                }
                is CoroutineState.CompleteHandler<*> -> {
                    (prev as CoroutineState.CompleteHandler<T>).doOnCompleted(result.getOrNull(), result.exceptionOrNull())
                    CoroutineState.Complete(result.getOrNull(), result.exceptionOrNull())
                }
                is CoroutineState.Complete<*> -> {
                    throw IllegalStateException("Already completed!")
                }
            }
        }

        (newState as CoroutineState.Complete<T>).exception?.let(this::handleException)
    }

    suspend fun join() {
        when (val currentState = state.get()) {
            is CoroutineState.InComplete -> return joinSuspend()
            is CoroutineState.Complete<*> -> return
            else -> throw IllegalStateException("Invalid State: $currentState")
        }
    }

    private suspend fun joinSuspend() = suspendCancellableCoroutine<Unit> { continuation ->
        doOnCompleted { t, throwable -> continuation.resume(Unit) }
    }

    override fun cancel() {
        val newState = state.updateAndGet { prev ->
            when (prev) {
                CoroutineState.InComplete,
                is CoroutineState.CompleteHandler<*> -> {
                    CoroutineState.Complete<T>(null, CancellationException("Coroutine is cancelled."))
                }
                else -> {
                    prev
                }
            }
        }

        if (newState is CoroutineState.Complete<*> && newState.exception is CancellationException) {
            cancelHandlers.forEach(CancelHandler::invoke)
        }
    }

    protected fun doOnCompleted(block: (T?, Throwable?) -> Unit) {
        val newState = state.updateAndGet { prev ->
            when (prev) {
                CoroutineState.InComplete -> {
                    CoroutineState.CompleteHandler<T>().also { it.invokeOnComplete(block) }
                }
                is CoroutineState.CompleteHandler<*> -> {
                    (prev as CoroutineState.CompleteHandler<T>).also { it.invokeOnComplete(block) }
                }
                is CoroutineState.Complete<*> -> {
                    prev
                }
            }
        }
        (newState as? CoroutineState.Complete<T>)?.let {
            block(it.value, it.exception)
        }
    }

    override fun invokeOnCancel(cancelHandler: CancelHandler) {
        cancelHandlers += cancelHandler
    }

    override fun invokeOnCompletion(completionHandler: CompletionHandler) {
        doOnCompleted { _, _ -> completionHandler() }
    }

    protected open fun handleException(e: Throwable) {}
}