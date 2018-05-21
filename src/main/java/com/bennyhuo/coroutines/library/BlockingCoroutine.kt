package com.bennyhuo.coroutines.library

import com.bennyhuo.coroutines.utils.log
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
            val task = eventQueue.take()
            task()
        }
    }
}