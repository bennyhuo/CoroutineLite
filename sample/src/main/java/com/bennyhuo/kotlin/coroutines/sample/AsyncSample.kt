package com.bennyhuo.kotlin.coroutines.sample

import com.bennyhuo.kotlin.coroutines.async
import com.bennyhuo.kotlin.coroutines.delay
import com.bennyhuo.kotlin.coroutines.launch
import com.bennyhuo.kotlin.coroutines.scope.GlobalScope
import com.bennyhuo.kotlin.coroutines.utils.log

suspend fun main() {
    GlobalScope.launch {
        log(1)
        val deferred = async {
            log(3)
            getValue()
        }
        log(2)
        val result = deferred.await()
        log(4, result)
    }.join()
    log(5)
}

suspend fun getValue(): String {
    delay(1000L)
    return "HelloWorld"
}