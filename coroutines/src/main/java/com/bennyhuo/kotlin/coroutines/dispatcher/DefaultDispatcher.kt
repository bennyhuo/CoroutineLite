package com.bennyhuo.kotlin.coroutines.dispatcher

import java.util.concurrent.*
import java.util.concurrent.atomic.*

object DefaultDispatcher: Dispatcher {

    private val threadGroup = ThreadGroup("DefaultDispatcher")

    private val threadIndex = AtomicInteger(0)

    private val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1) { runnable ->
        Thread(threadGroup, runnable, "${threadGroup.name}-worker-${threadIndex.getAndIncrement()}").apply { isDaemon = true }
    }

    override fun dispatch(block: () -> Unit) {
        executor.submit(block)
    }
}