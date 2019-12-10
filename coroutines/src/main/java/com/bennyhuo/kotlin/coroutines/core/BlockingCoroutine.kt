package com.bennyhuo.kotlin.coroutines.core

import com.bennyhuo.kotlin.coroutines.dispatcher.Dispatcher
import java.util.concurrent.LinkedBlockingDeque
import kotlin.coroutines.CoroutineContext

typealias EventTask = () -> Unit

class BlockingQueueDispatcher : LinkedBlockingDeque<EventTask>(), Dispatcher {

    override fun dispatch(block: EventTask) {
        offer(block)
    }
}

class BlockingCoroutine<T>(context: CoroutineContext, private val eventQueue: LinkedBlockingDeque<EventTask>) : AbstractCoroutine<T>(context) {

    fun joinBlocking(): T {
        while (!isCompleted) {
            eventQueue.take().invoke()
        }
        return (state.get() as CoroutineState.Complete<T>).let {
            it.value ?: throw it.exception!!
        }
    }
}