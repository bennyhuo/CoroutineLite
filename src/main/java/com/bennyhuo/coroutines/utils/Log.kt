package com.bennyhuo.coroutines.utils

import com.bennyhuo.coroutines.lite.CoroutineScope
import com.bennyhuo.coroutines.lite.Job
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext

/**
 * Created by benny on 5/20/17.
 */
val dateFormat = SimpleDateFormat("HH:mm:ss:SSS")

val now = {
    dateFormat.format(Date(System.currentTimeMillis()))
}

fun log(vararg msg: Any?) = println("${now()} [${Thread.currentThread().name}] ${msg.joinToString(" ")}")

fun stackTrace(){
    Throwable().printStackTrace(System.out)
}

fun CoroutineScope.log(vararg msg: Any?) = coroutineContext.log(*msg)

fun <T> Continuation<T>.log(vararg msg: Any?) = context.log(*msg)

fun CoroutineContext.log(vararg msg: Any?) = println("${now()} [${Thread.currentThread().name} ${this[Job]}] ${msg.joinToString(" ")}")