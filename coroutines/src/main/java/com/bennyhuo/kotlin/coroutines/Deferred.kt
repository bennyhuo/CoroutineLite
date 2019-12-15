package com.bennyhuo.kotlin.coroutines

import com.bennyhuo.kotlin.coroutines.core.AbstractCoroutine
import com.bennyhuo.kotlin.coroutines.core.CoroutineState
import com.bennyhuo.kotlin.coroutines.cancel.suspendCancellableCoroutine
import kotlin.coroutines.CoroutineContext

interface Deferred<T>: Job {

    suspend fun await(): T

}