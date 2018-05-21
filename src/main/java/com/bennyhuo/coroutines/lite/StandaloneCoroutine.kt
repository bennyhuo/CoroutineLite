package com.bennyhuo.coroutines.lite

import kotlin.coroutines.experimental.CoroutineContext

class StandaloneCoroutine<T>(context: CoroutineContext, block: suspend () -> T): AbstractCoroutine<T>(context, block)