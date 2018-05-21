package com.bennyhuo.coroutines.lite

import java.util.concurrent.LinkedBlockingDeque
import kotlin.coroutines.experimental.CoroutineContext

typealias EventTask = () -> Unit

class BlockingQueueDispatcher : LinkedBlockingDeque<EventTask>(), Dispatcher {

    override fun dispatch(block: EventTask) {
        offer(block)
    }
}

class BlockingCoroutine<T>(context: CoroutineContext, private val eventQueue: LinkedBlockingDeque<EventTask>, block: suspend () -> T) : AbstractCoroutine<T>(context, block) {

    fun joinBlocking() {
        while (!isCompleted) {
            eventQueue.take().invoke()
        }
    }
}