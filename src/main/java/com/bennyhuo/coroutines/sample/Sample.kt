package com.bennyhuo.coroutines.sample

import com.bennyhuo.coroutines.async
import com.bennyhuo.coroutines.delay
import com.bennyhuo.coroutines.launch
import com.bennyhuo.coroutines.utils.log

fun main(args: Array<String>)  {
    launch {
        log(-1)
        val result = async {
            log(1)
            loadForResult().also {
                log(2)
            }
        }
        log(-2)
        log(result.await())
        log(-3)
    }

    Thread.sleep(10000)
}

suspend fun loadForResult(): String {
    delay(1000L)
    return "HelloWorld"
}