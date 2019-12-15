package com.bennyhuo.kotlin.coroutines.core

import com.bennyhuo.kotlin.coroutines.*
import com.bennyhuo.kotlin.coroutines.cancel.suspendCancellableCoroutine
import com.bennyhuo.kotlin.coroutines.context.CoroutineName
import com.bennyhuo.kotlin.coroutines.scope.CoroutineScope
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume

abstract class AbstractCoroutine<T>(context: CoroutineContext) : Job, Continuation<T>, CoroutineScope {

    protected val state = AtomicReference<CoroutineState>()

    override val context: CoroutineContext

    override val coroutineContext: CoroutineContext
        get() = context

    protected val parentJob = context[Job]

    private var parentCancelDisposable: Disposable? = null

    init {
        state.set(CoroutineState.InComplete())
        this.context = context + this

        parentCancelDisposable = parentJob?.invokeOnCancel {
            cancel()
        }
    }

    val isCompleted
        get() = state.get() is CoroutineState.Complete<*>

    override val isActive: Boolean
        get() = when(state.get()){
            is CoroutineState.Complete<*>,
            is CoroutineState.Cancelling -> false
            else -> true
        }

    override fun resumeWith(result: Result<T>) {
        val newState = state.updateAndGet { prevState ->
            when(prevState){
                //although cancelled, flows of job may work out with the normal result.
                is CoroutineState.Cancelling,
                is CoroutineState.InComplete -> {
                    CoroutineState.Complete(result.getOrNull(), result.exceptionOrNull()).from(prevState)
                }
                is CoroutineState.Complete<*> -> {
                    throw IllegalStateException("Already completed!")
                }
            }
        }

        (newState as CoroutineState.Complete<T>).exception?.let(this::tryHandleException)

        newState.notifyCompletion(result)
        newState.clear()
        parentCancelDisposable?.dispose()
    }

    override suspend fun join() {
        when (state.get()) {
            is CoroutineState.InComplete,
            is CoroutineState.Cancelling -> return joinSuspend()
            is CoroutineState.Complete<*> -> {
                val parentJobState = this.parentJob?.isActive ?: return
                if(!parentJobState){
                    throw CancellationException("Parent cancelled.")
                }
                return
            }
        }
    }

    private suspend fun joinSuspend() = suspendCancellableCoroutine<Unit> { continuation ->
        val disposable = doOnCompleted { result ->
            continuation.resume(Unit)
        }
        continuation.invokeOnCancel { disposable.dispose() }
    }

    override fun cancel() {
        val newState = state.updateAndGet { prev ->
            when (prev) {
                is CoroutineState.InComplete -> {
                    CoroutineState.Cancelling().from(prev)
                }
                is CoroutineState.Cancelling,
                is CoroutineState.Complete<*> -> prev
            }
        }

        if(newState is CoroutineState.Cancelling){
            newState.notifyCancellation()
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
            }
        }
        (newState as? CoroutineState.Complete<T>)?.let {
            block(
                    when {
                        it.value != null -> Result.success(it.value)
                        it.exception != null -> Result.failure(it.exception)
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
            }
        }
        (newState as? CoroutineState.Cancelling)?.let {
            // call immediately when complete.
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
            }
        }
    }

    private fun tryHandleException(e: Throwable): Boolean{
        return when(e){
            is CancellationException -> {
                false
            }
            else -> {
                (parentJob as? AbstractCoroutine<*>)?.handleChildException(e)?.takeIf { it }
                        ?: handleJobException(e)
            }
        }
    }

    protected open fun handleChildException(e: Throwable): Boolean{
        cancel()
        return tryHandleException(e)
    }

    protected open fun handleJobException(e: Throwable) = false

    override fun toString(): String {
        return context[CoroutineName].toString()
    }
}