package com.bennyhuo.kotlin.coroutines

import com.bennyhuo.kotlin.coroutines.core.Disposable
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

    fun attachChild(child: Job): Disposable

    suspend fun join()
}