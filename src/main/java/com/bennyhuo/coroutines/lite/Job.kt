package com.bennyhuo.coroutines.lite

import kotlin.coroutines.CoroutineContext

typealias CompletionHandler = () -> Unit

typealias CancellationException = java.util.concurrent.CancellationException
typealias CancelHandler = () -> Unit

interface Job : CoroutineContext.Element {
    companion object Key : CoroutineContext.Key<Job>

    override val key: CoroutineContext.Key<*> get() = Job

    val isActive: Boolean

    fun invokeOnCancel(cancelHandler: CancelHandler)

    fun invokeOnCompletion(completionHandler: CompletionHandler)

    fun cancel()

    suspend fun join()
}