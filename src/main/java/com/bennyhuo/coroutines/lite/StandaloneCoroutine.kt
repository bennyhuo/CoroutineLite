package com.bennyhuo.coroutines.lite

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

interface CoroutineExceptionHandler : CoroutineContext.Element {

    companion object Key : CoroutineContext.Key<CoroutineExceptionHandler>

    fun handleException(context: CoroutineContext, exception: Throwable)
}

inline fun CoroutineExceptionHandler(crossinline handler: (CoroutineContext, Throwable) -> Unit): CoroutineExceptionHandler =
        object : AbstractCoroutineContextElement(CoroutineExceptionHandler), CoroutineExceptionHandler {
            override fun handleException(context: CoroutineContext, exception: Throwable) =
                    handler.invoke(context, exception)
        }

class StandaloneCoroutine(context: CoroutineContext, block: suspend () -> Unit) : AbstractCoroutine<Unit>(context, block) {

    override fun handleException(e: Throwable) {
        super.handleException(e)
        context[CoroutineExceptionHandler]?.handleException(context, e)
    }

}