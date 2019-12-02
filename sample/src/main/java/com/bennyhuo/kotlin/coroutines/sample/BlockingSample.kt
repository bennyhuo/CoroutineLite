package com.bennyhuo.kotlin.coroutines.sample

import com.bennyhuo.kotlin.coroutines.delay
import com.bennyhuo.kotlin.coroutines.launch
import com.bennyhuo.kotlin.coroutines.runBlocking
import com.bennyhuo.kotlin.coroutines.utils.log

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