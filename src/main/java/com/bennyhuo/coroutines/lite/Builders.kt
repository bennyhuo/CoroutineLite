package com.bennyhuo.coroutines.lite

import kotlin.coroutines.experimental.CoroutineContext

/**
 * Created by benny on 5/20/17.
 */
fun launch(context: CoroutineContext = CommonPool, block: suspend () -> Unit): AbstractCoroutine<Unit> {
    return StandaloneCoroutine(context, block)
}

fun <T> async(context: CoroutineContext = CommonPool, block: suspend () -> T): Deferred<T> {
    return Deferred(context, block)
}

fun runBlocking(block: suspend () -> Unit) {
    val eventQueue = BlockingQueueDispatcher()
    val context = DispatcherContext(eventQueue)
    BlockingCoroutine(context, eventQueue, block).joinBlocking()
}

