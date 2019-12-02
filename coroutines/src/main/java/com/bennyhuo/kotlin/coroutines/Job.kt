package com.bennyhuo.kotlin.coroutines

import kotlin.coroutines.CoroutineContext

typealias OnComplete = () -> Unit

typealias CancellationException = java.util.concurrent.CancellationException
typealias OnCancel = () -> Unit

interface Job : CoroutineContext.Element {
    companion object Key : CoroutineContext.Key<Job>

    override val key: CoroutineContext.Key<*> get() = Job

    val isActive: Boolean

    fun invokeOnCancel(onCancel: OnCancel): Disposable

    fun invokeOnCompletion(onComplete: OnComplete): Disposable

    fun cancel()

    fun remove(disposable: Disposable)

    suspend fun join()
}