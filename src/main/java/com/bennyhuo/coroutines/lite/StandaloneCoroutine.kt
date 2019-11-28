package com.bennyhuo.coroutines.lite

import kotlin.coroutines.CoroutineContext

interface CoroutineExceptionHandler : CoroutineContext.Element {

    companion object Key : CoroutineContext.Key<CoroutineExceptionHandler>

    fun handleException(context: CoroutineContext, exception: Throwable)
}

class StandaloneCoroutine(context: CoroutineContext, block: suspend () -> Unit): AbstractCoroutine<Unit>(context, block) {

    override fun handleException(e: Throwable) {
        super.handleException(e)
        context[CoroutineExceptionHandler]?.handleException(context, e)
    }

}