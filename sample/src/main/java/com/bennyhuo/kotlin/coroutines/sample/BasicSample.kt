package com.bennyhuo.kotlin.coroutines.sample

import com.bennyhuo.kotlin.coroutines.scope.GlobalScope
import com.bennyhuo.kotlin.coroutines.delay
import com.bennyhuo.kotlin.coroutines.launch
import kotlin.coroutines.suspendCoroutine

suspend fun main() {
    GlobalScope.launch {

        test()

        test()

        test()

    }.join()
}

suspend fun loadForResult(): String {
    delay(1000L)
    return "HelloWorld"
}

suspend fun test() = suspendCoroutine<Unit> {
    println(it.hashCode())
    it.resumeWith(Result.success(Unit))
}