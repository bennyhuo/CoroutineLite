package com.bennyhuo.coroutines.sample

import com.bennyhuo.coroutines.lite.GlobalScope
import com.bennyhuo.coroutines.lite.launch
import com.bennyhuo.coroutines.lite.suspendCancellableCoroutine
import com.bennyhuo.coroutines.utils.log
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.random.Random

suspend fun main() {
    log(0)
    val job = GlobalScope.launch {
        log(1)
        val r0 = nonCancellableFunction()
        log(2, r0)
        val r1 = cancellableFunction()
        log(3, r1)
    }
    log(4)

    job.cancel()
    job.join()
}

suspend fun nonCancellableFunction() = suspendCoroutine<Int> { continuation ->

    val completableFuture = CompletableFuture.supplyAsync {
        Thread.sleep(1000)
        Random.nextInt()
    }

    completableFuture.thenApply {
        continuation.resume(it)
    }.exceptionally {
        continuation.resumeWithException(it)
    }
}

suspend fun cancellableFunction() = suspendCancellableCoroutine<Int> { continuation ->

    val completableFuture = CompletableFuture.supplyAsync {
        Thread.sleep(1000)
        Random.nextInt()
    }

    continuation.invokeOnCancel {
        completableFuture.cancel(true)
    }

    completableFuture.thenApply {
        continuation.resume(it)
    }.exceptionally {
        continuation.resumeWithException(it)
    }
}

