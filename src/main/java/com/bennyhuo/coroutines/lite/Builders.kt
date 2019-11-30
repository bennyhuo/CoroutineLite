package com.bennyhuo.coroutines.lite

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.startCoroutine

/**
 * Created by benny on 5/20/17.
 */
fun CoroutineScope.launch(context: CoroutineContext = CommonPool, block: suspend CoroutineScope.() -> Unit): Job {
    val completion = StandaloneCoroutine(context)
    block.startCoroutine(completion, completion)
    return completion
}

fun <T> CoroutineScope.async(context: CoroutineContext = CommonPool, block: suspend CoroutineScope.() -> T): Deferred<T> {
    val completion = Deferred<T>(context)
    block.startCoroutine(completion, completion)
    return completion
}

fun <T> runBlocking(block: suspend CoroutineScope.() -> T): T {
    val eventQueue = BlockingQueueDispatcher()
    val context = DispatcherContext(eventQueue)
    val completion = BlockingCoroutine<T>(context, eventQueue)
    block.startCoroutine(completion, completion)
    return completion.joinBlocking()
}

