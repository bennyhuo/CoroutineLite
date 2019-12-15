package com.bennyhuo.kotlin.coroutines.sample

import com.bennyhuo.kotlin.coroutines.delay
import com.bennyhuo.kotlin.coroutines.launch
import com.bennyhuo.kotlin.coroutines.scope.GlobalScope
import com.bennyhuo.kotlin.coroutines.utils.log
import kotlin.concurrent.thread
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun main(){
    val job = GlobalScope.launch {
        log(1)
        val result = hello()
        log(2, result)
        delay(1000)
        log(3)
    }

    log(job.isActive)
    job.cancel()
    job.join()
}

suspend fun hello() = suspendCoroutine<Int> {
    thread(isDaemon = true) {
        Thread.sleep(1000)
        it.resume(10086)
    }
}