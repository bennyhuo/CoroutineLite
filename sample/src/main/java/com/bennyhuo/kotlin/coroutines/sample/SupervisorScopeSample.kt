package com.bennyhuo.kotlin.coroutines.sample

import com.bennyhuo.kotlin.coroutines.utils.log
import com.bennyhuo.kotlin.coroutines.*

suspend fun main(){

    val exceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        log(coroutineContext[CoroutineName], throwable)
    }

    GlobalScope.launch(exceptionHandler) {
        log(1)
        supervisorScope {
            log(2)
            launch(exceptionHandler) {
                log(3)
                launch(exceptionHandler) {
                    log(4)
                    throw IllegalStateException()
                }.join()
                log(5)
            }.join()
            log(6)
        }
        log(7)
    }.join()
    log(8)
}