package com.bennyhuo.kotlin.coroutines.core

import com.bennyhuo.kotlin.coroutines.Job

sealed class CoroutineState {
    protected var disposableList: RecursiveList<Disposable> = RecursiveList.Nil
        private set
    protected var children: RecursiveList<Job> = RecursiveList.Nil
        private set

    fun from(state: CoroutineState): CoroutineState {
        this.disposableList = state.disposableList
        this.children = state.children
        return this
    }

    fun with(element: Any): CoroutineState {
        when(element){
            is Disposable -> this.disposableList = RecursiveList.Cons(element, this.disposableList)
            is Job -> this.children = RecursiveList.Cons(element, this.children)
        }
        return this
    }

    fun without(element: Any): CoroutineState {
        when(element){
            is Disposable -> this.disposableList = this.disposableList.remove(element)
            is Job -> this.children = this.children.remove(element)
        }
        return this
    }

    fun <T> notifyCompletion(result: Result<T>) {
        this.disposableList.loopOn<CompletionHandlerDisposable<T>> {
            it.onComplete(result)
        }
    }

    fun notifyCancellation() {
        disposableList.loopOn<CancellationHandlerDisposable> {
            it.onCancel()
        }
    }

    fun clear() {
        this.disposableList = RecursiveList.Nil
        this.children = RecursiveList.Nil
    }

    fun <T> tryComplete(result: Result<T>): CoroutineState {
        return if(children == RecursiveList.Nil) Complete(result.getOrNull(), result.exceptionOrNull()).from(this)
        else CompleteWaitForChildren(result.getOrNull(), result.exceptionOrNull(), this is Cancelling).from(this)
    }

    override fun toString(): String {
        return "CoroutineState.${this.javaClass.simpleName}"
    }

    class InComplete : CoroutineState()
    class Cancelling: CoroutineState()
    class CompleteWaitForChildren<T>(val value: T? = null, val exception: Throwable? = null, val isCancelling: Boolean = false) : CoroutineState(){
        fun copy(value: T? = this.value, exception: Throwable? = this.exception, isCancelling: Boolean = this.isCancelling): CompleteWaitForChildren<T>{
            return CompleteWaitForChildren(value, exception, isCancelling).from(this) as CompleteWaitForChildren<T>
        }

        fun tryWaitForChildren(onChildComplete: (Job) -> Unit){
            children.forEach { child ->
                child.invokeOnCompletion {
                    onChildComplete(child)
                }
            }
        }

        fun onChildCompleted(job: Job): CoroutineState {
            when(val currentChildren = children){
                is RecursiveList.Cons -> {
                    if(currentChildren.tail == RecursiveList.Nil && currentChildren.head == job){
                        return Complete(value, exception).from(this)
                    }
                }
                else -> {}
            }
            return CompleteWaitForChildren(value, exception, isCancelling).from(this).without(job)
        }
    }
    class Complete<T>(val value: T? = null, val exception: Throwable? = null) : CoroutineState()
}
