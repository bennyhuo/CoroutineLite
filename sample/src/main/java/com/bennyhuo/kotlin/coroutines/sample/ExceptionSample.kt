package com.bennyhuo.kotlin.coroutines.sample

import com.bennyhuo.kotlin.coroutines.exception.CoroutineExceptionHandler
import com.bennyhuo.kotlin.coroutines.context.CoroutineName
import com.bennyhuo.kotlin.coroutines.scope.GlobalScope
import com.bennyhuo.kotlin.coroutines.launch
import com.bennyhuo.kotlin.coroutines.utils.log

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