package com.bennyhuo.kotlin.coroutines.core

import com.bennyhuo.kotlin.coroutines.*
import com.bennyhuo.kotlin.coroutines.cancel.suspendCancellableCoroutine
import com.bennyhuo.kotlin.coroutines.context.CoroutineName
import com.bennyhuo.kotlin.coroutines.scope.CoroutineScope
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume

abstract class AbstractCoroutine<T>(context: CoroutineContext) : Job, Continuation<T>, CoroutineScope {

    protected val state = AtomicReference<CoroutineState>()

    final override val context: CoroutineContext

    override val scopeContext: CoroutineContext
        get() = context

    protected val parentJob = context[Job]

    private var parentCancelDisposable: Disposable? = null

    init {
        state.set(CoroutineState.InComplete())
        this.context = context + this

        parentCancelDisposable = parentJob?.attachChild(this)
    }

    val isCompleted
        get() = state.get() is CoroutineState.Complete<*>

    override val isActive: Boolean
        get() = when (val currentState = state.get()) {
            is CoroutineState.Complete<*>,
            is CoroutineState.Cancelling -> false
            is CoroutineState.CompleteWaitForChildren<*> -> !currentState.isCancelling
            is CoroutineState.InComplete -> true
        }

    override fun resumeWith(result: Result<T>) {
        val newState = state.updateAndGet { prevState ->
            when (prevState) {
                //although cancelled, flows of job may work out with the normal result.
                is CoroutineState.Cancelling,
                is CoroutineState.InComplete -> prevState.tryComplete(result)
                is CoroutineState.CompleteWaitForChildren<*>,
                is CoroutineState.Complete<*> -> {
                    throw IllegalStateException("Already completed!")
                }
            }
        }

        when(newState){
            is CoroutineState.CompleteWaitForChildren<*> -> newState.tryWaitForChildren(::tryCompleteOnChildCompleted)
            is CoroutineState.Complete<*> -> makeCompletion(newState as CoroutineState.Complete<T>)
            else -> {}
        }

    }

    private fun tryCompleteOnChildCompleted(child: Job) {
        val newState = state.updateAndGet { prev ->
            when (prev) {
                is CoroutineState.Cancelling,
                is CoroutineState.InComplete -> {
                    throw IllegalStateException("Should be waiting for children!")
                }
                is CoroutineState.CompleteWaitForChildren<*> -> {
                    prev.onChildCompleted(child)
                }
                is CoroutineState.Complete<*> -> throw IllegalStateException("Already completed!")
            }
        }

        (newState as? CoroutineState.Complete<T>)?.let {
            makeCompletion(it)
        }
    }

    private fun makeCompletion(newState: CoroutineState.Complete<T>){
        val result = if (newState.exception == null) {
            Result.success(newState.value)
        } else {
            Result.failure<T>(newState.exception)
        }

        result.exceptionOrNull()?.let(this::tryHandleException)

        newState.notifyCompletion(result)
        newState.clear()
        parentCancelDisposable?.dispose()
    }

    override suspend fun join() {
        when (state.get()) {
            is CoroutineState.InComplete,
            is CoroutineState.CompleteWaitForChildren<*>,
            is CoroutineState.Cancelling -> return joinSuspend()
            is CoroutineState.Complete<*> -> {
                val currentCallingJobState = coroutineContext[Job]?.isActive ?: return
                if (!currentCallingJobState) {
                    throw CancellationException("Coroutine is cancelled.")
                }
                return
            }
        }
    }

    private suspend fun joinSuspend() = suspendCancellableCoroutine<Unit> { continuation ->
        val disposable = doOnCompleted { result ->
            continuation.resume(Unit)
        }
        continuation.invokeOnCancellation { disposable.dispose() }
    }

    override fun cancel() {
        val prevState = state.getAndUpdate { prev ->
            when (prev) {
                is CoroutineState.InComplete -> {
                    CoroutineState.Cancelling().from(prev)
                }
                is CoroutineState.Cancelling,
                is CoroutineState.Complete<*> -> prev
                is CoroutineState.CompleteWaitForChildren<*> -> {
                    prev.copy(isCancelling = true)
                }
            }
        }

        if (prevState is CoroutineState.InComplete) {
            prevState.notifyCancellation()
        }
        parentCancelDisposable?.dispose()
    }

    protected fun doOnCompleted(block: (Result<T>) -> Unit): Disposable {
        val disposable = CompletionHandlerDisposable(this, block)
        val newState = state.updateAndGet { prev ->
            when (prev) {
                is CoroutineState.InComplete -> {
                    CoroutineState.InComplete().from(prev).with(disposable)
                }
                is CoroutineState.Cancelling -> {
                    CoroutineState.Cancelling().from(prev).with(disposable)
                }
                is CoroutineState.Complete<*> -> {
                    prev
                }
                is CoroutineState.CompleteWaitForChildren<*> -> prev.copy().with(disposable)
            }
        }
        (newState as? CoroutineState.Complete<T>)?.let {
            block(
                    when {
                        it.exception != null -> Result.failure(it.exception)
                        it.value != null -> Result.success(it.value)
                        else -> throw IllegalStateException("Won't happen.")
                    }
            )
        }
        return disposable
    }

    override fun invokeOnCancel(onCancel: OnCancel): Disposable {
        val disposable = CancellationHandlerDisposable(this, onCancel)
        val newState = state.updateAndGet { prev ->
            when (prev) {
                is CoroutineState.InComplete -> {
                    CoroutineState.InComplete().from(prev).with(disposable)
                }
                is CoroutineState.Cancelling,
                is CoroutineState.Complete<*> -> {
                    prev
                }
                is CoroutineState.CompleteWaitForChildren<*> -> prev.copy().with(disposable)
            }
        }
        (newState as? CoroutineState.Cancelling)?.let {
            // call immediately when Cancelling.
            onCancel()
        }
        return disposable
    }

    override fun invokeOnCompletion(onComplete: OnComplete): Disposable {
        return doOnCompleted { _ -> onComplete() }
    }

    override fun remove(disposable: Disposable) {
        state.updateAndGet { prev ->
            when (prev) {
                is CoroutineState.InComplete -> {
                    CoroutineState.InComplete().from(prev).without(disposable)
                }
                is CoroutineState.Cancelling -> {
                    CoroutineState.Cancelling().from(prev).without(disposable)
                }
                is CoroutineState.Complete<*> -> {
                    prev
                }
                is CoroutineState.CompleteWaitForChildren<*> -> prev.copy().without(disposable)
            }
        }
    }

    private fun tryHandleException(e: Throwable): Boolean {
        return when (e) {
            is CancellationException -> {
                false
            }
            else -> {
                (parentJob as? AbstractCoroutine<*>)?.handleChildException(e)?.takeIf { it }
                        ?: handleJobException(e)
            }
        }
    }

    protected open fun handleChildException(e: Throwable): Boolean {
        cancel()
        return tryHandleException(e)
    }

    protected open fun handleJobException(e: Throwable) = false

    override fun attachChild(child: Job): Disposable {
        state.updateAndGet { prev ->
            when(prev) {
                is CoroutineState.InComplete -> {
                    CoroutineState.InComplete().from(prev).with(child)
                }
                is CoroutineState.Cancelling -> {
                    CoroutineState.Cancelling().from(prev).with(child)
                }
                is CoroutineState.CompleteWaitForChildren<*> ->  prev.copy().with(child)
                is CoroutineState.Complete<*> -> throw IllegalStateException("Parent already completed.")
            }
        }

        return invokeOnCancel {
            child.cancel()
        }
    }

    override fun toString(): String {
        return context[CoroutineName].toString()
    }
}
