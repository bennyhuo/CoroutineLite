package com.bennyhuo.coroutines.sample

import com.bennyhuo.coroutines.library.delay
import com.bennyhuo.coroutines.library.launch
import com.bennyhuo.coroutines.library.runBlocking
import com.bennyhuo.coroutines.utils.log
import java.util.concurrent.atomic.AtomicInteger

fun main(args: Array<String>) = runBlocking {
    log(1)
    val job = launch {
        log(1)
        delay(1000L)
        log(2)
    }
    log(2)
    job.join()
    log(3)
}