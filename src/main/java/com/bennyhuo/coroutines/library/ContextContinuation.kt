package com.bennyhuo.coroutines

import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.EmptyCoroutineContext

class ContextContinuation<T>(override val context: CoroutineContext = EmptyCoroutineContext) : Continuation<T> {

    override fun resume(value: T) {}

    override fun resumeWithException(exception: Throwable) {
        exception.printStackTrace()
    }
}