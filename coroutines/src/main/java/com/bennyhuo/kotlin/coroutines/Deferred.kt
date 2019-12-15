package com.bennyhuo.kotlin.coroutines

interface Deferred<T>: Job {

    suspend fun await(): T

}