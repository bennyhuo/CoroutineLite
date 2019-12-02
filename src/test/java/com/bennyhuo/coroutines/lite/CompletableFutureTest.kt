package com.bennyhuo.coroutines.lite

import com.bennyhuo.coroutines.utils.log
import org.junit.Test
import java.util.concurrent.CompletableFuture
import kotlin.random.Random

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
            log("exception", it)
        }
    }
}