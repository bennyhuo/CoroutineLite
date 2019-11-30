package com.bennyhuo.coroutines.sample

import com.bennyhuo.coroutines.lite.CoroutineExceptionHandler
import com.bennyhuo.coroutines.lite.CoroutineName
import com.bennyhuo.coroutines.lite.GlobalScope
import com.bennyhuo.coroutines.lite.launch
import com.bennyhuo.coroutines.utils.log

suspend fun main() {

    val exceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        log("[ExceptionHandler]", coroutineContext[CoroutineName], throwable.message)
    }

    val context = exceptionHandler + CoroutineName("MyCoroutine")

    GlobalScope.launch(context) {
        log(1)
        throw ArithmeticException("Div by 0")
        log(2)
    }.join()
}