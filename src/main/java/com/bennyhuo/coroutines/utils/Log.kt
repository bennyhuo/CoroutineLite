package com.bennyhuo.coroutines.utils

import com.bennyhuo.coroutines.lite.CoroutineScope
import com.bennyhuo.coroutines.lite.Job
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by benny on 5/20/17.
 */
val dateFormat = SimpleDateFormat("HH:mm:ss:SSS")

val now = {
    dateFormat.format(Date(System.currentTimeMillis()))
}

fun log(vararg msg: Any?) = println("${now()} [${Thread.currentThread().name}] ${msg.joinToString(" ")}")

fun CoroutineScope.log(vararg msg: Any?) = println("${now()} [${Thread.currentThread().name} ${coroutineContext[Job]}] ${msg.joinToString(" ")}")