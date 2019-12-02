package com.bennyhuo.kotlin.coroutines

import com.bennyhuo.kotlin.coroutines.utils.log
import org.junit.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import kotlin.random.Random
import kotlin.test.expect

class CompletableFutureTest {
    @Test
    fun test(){
        val completableFuture = CompletableFuture.supplyAsync {
            Thread.sleep(1000)
            Random.nextInt()
        }

        completableFuture.cancel(false)

        completableFuture.thenApply {
            log(it)
        }.exceptionally {
            assert(it is CompletionException)
        }
    }
}