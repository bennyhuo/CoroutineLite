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
        launch {
            delay(1000)
        }.join()
        log(2)
        delay(1000)
        log(3)
    }

    log(job.isActive)
    Thread.sleep(500)
    job.cancel()
    job.join()
    log("end")
}
