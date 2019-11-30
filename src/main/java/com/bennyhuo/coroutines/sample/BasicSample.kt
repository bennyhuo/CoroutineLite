package com.bennyhuo.coroutines.sample

import com.bennyhuo.coroutines.lite.*
import com.bennyhuo.coroutines.utils.log
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