package com.bennyhuo.coroutines

import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.startCoroutine

/**
 * Created by benny on 5/20/17.
 */
fun launch(context: CoroutineContext = CommonPool, block: suspend () -> Unit) {
    block.startCoroutine(ContextContinuation(context))
}

fun <T> async(context: CoroutineContext = CommonPool, block: suspend () -> T): Deferred<T> {
    return Deferred(context, block)
}