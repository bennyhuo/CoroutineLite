package com.bennyhuo.coroutines.lite

import com.bennyhuo.coroutines.utils.log
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.coroutines.resume

typealias OnComplete<T> = (T?, Throwable?) -> Unit

sealed class CoroutineState {
    object InComplete : CoroutineState()
    class Complete<T>(val value: T? = null, val exception: Throwable? = null) : CoroutineState()
    class CompleteHandler<T> : CoroutineState() {

        private val handlers = CopyOnWriteArrayList<OnComplete<T>>()

        @Volatile var isCancelling = false
            private set

        fun invokeOnComplete(onComplete: OnComplete<T>) {
            handlers += onComplete
        }

        fun makeCancelling() {
            this.isCancelling = true
        }

        fun doOnCompleted(value: T?, throwable: Throwable?) {
            handlers.forEach {
                it.invoke(value, throwable)
            }
        }
    }
}

abstract class AbstractCoroutine<T>(context: CoroutineContext, block: suspend () -> T) : Job, Continuation<T> {

    protected val state = AtomicReference<CoroutineState>()

    override val context: CoroutineContext

    init {
        state.set(CoroutineState.InComplete)
        this.context = context + this
        block.startCoroutine(this)
    }

    val isCompleted
        get() = state.get() is CoroutineState.Complete<*>

    override val isActive: Boolean
        get() = when(val currentState = state.get()){
            CoroutineState.InComplete -> true
            is CoroutineState.Complete<*> -> false
            is CoroutineState.CompleteHandler<*> -> !currentState.isCancelling
        }

    private val cancelHandlers = CopyOnWriteArrayList<CancelHandler>()

    override fun resumeWith(result: Result<T>) {
        val newState = state.updateAndGet { prev ->
            when (prev) {
                CoroutineState.InComplete -> {
                    CoroutineState.Complete(result.getOrNull(), result.exceptionOrNull())
                }
                is CoroutineState.CompleteHandler<*> -> {
                    (prev as CoroutineState.CompleteHandler<T>).doOnCompleted(result.getOrNull(), result.exceptionOrNull())
                    if(prev.isCancelling){
                        CoroutineState.Complete(null, CancellationException("Result arrived, but cancelled already."))
                    } else {
                        CoroutineState.Complete(result.getOrNull(), result.exceptionOrNull())
                    }
                }
                is CoroutineState.Complete<*> -> {
                    throw IllegalStateException("Already completed!")
                }
            }
        }

        (newState as CoroutineState.Complete<T>).exception?.let(this::handleException)
    }

    override suspend fun join() {
        when (state.get()) {
            CoroutineState.InComplete,
            is CoroutineState.CompleteHandler<*> -> return joinSuspend()
            is CoroutineState.Complete<*> -> return
        }
    }

    private suspend fun joinSuspend() = suspendCancellableCoroutine<Unit> { continuation ->
        doOnCompleted { t, throwable -> continuation.resume(Unit) }
    }

    override fun cancel() {
        val newState = state.updateAndGet { prev ->
            when (prev) {
                CoroutineState.InComplete -> {
                    CoroutineState.CompleteHandler<T>().also { it.makeCancelling() }
                }
                is CoroutineState.CompleteHandler<*> -> {
                    prev.also { it.makeCancelling() }
                }
                is CoroutineState.Complete<*> -> prev
            }
        }

        if (newState is CoroutineState.CompleteHandler<*> && newState.isCancelling) {
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
        when(val currentState = state.get()){
            CoroutineState.InComplete -> cancelHandlers += cancelHandler
            is CoroutineState.CompleteHandler<*> -> {
                if(currentState.isCancelling) {
                    cancelHandler()
                } else {
                    cancelHandlers += cancelHandler
                }
            }
        }
    }

    override fun invokeOnCompletion(completionHandler: CompletionHandler) {
        doOnCompleted { _, _ -> completionHandler() }
    }

    protected open fun handleException(e: Throwable) {}
}