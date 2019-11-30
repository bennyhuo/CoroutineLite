package com.bennyhuo.coroutines.lite

import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.startCoroutine

private var coroutineIndex = AtomicInteger(0)

fun CoroutineScope.launch(context: CoroutineContext = CommonPool, block: suspend CoroutineScope.() -> Unit): Job {
    val completion = StandaloneCoroutine(newCoroutineContext(context))
    block.startCoroutine(completion, completion)
    return completion
}

fun <T> CoroutineScope.async(context: CoroutineContext = CommonPool, block: suspend CoroutineScope.() -> T): Deferred<T> {
    val completion = Deferred<T>(newCoroutineContext(context))
    block.startCoroutine(completion, completion)
    return completion
}

fun CoroutineScope.newCoroutineContext(context: CoroutineContext): CoroutineContext {
    val combined = coroutineContext + context + CoroutineName("@coroutine-${coroutineIndex.getAndIncrement()}")
    return if (combined !== CommonPool && combined[ContinuationInterceptor] == null) combined + CommonPool else combined
}

fun <T> runBlocking(block: suspend CoroutineScope.() -> T): T {
    val eventQueue = BlockingQueueDispatcher()
    val context = DispatcherContext(eventQueue)
    val completion = BlockingCoroutine<T>(context, eventQueue)
    block.startCoroutine(completion, completion)
    return completion.joinBlocking()
}

