package com.bennyhuo.kotlin.coroutines.sample

import com.bennyhuo.kotlin.coroutines.scope.GlobalScope
import com.bennyhuo.kotlin.coroutines.launch
import com.bennyhuo.kotlin.coroutines.cancel.suspendCancellableCoroutine
import com.bennyhuo.kotlin.coroutines.utils.log
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
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

    job.invokeOnCancel {
        log("invoke on cancel.")
    }

    job.cancel()
    job.cancel()
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
    continuation.invokeOnCancellation {
        completableFuture.cancel(true)
    }
    completableFuture.thenApply {
        continuation.resume(it)
    }.exceptionally {
        // when cancelled, `it` will be a CompletionException wrapping a CancellationException.
        continuation.resumeWithException((it as? CompletionException)?.cause ?: it)
    }
}
