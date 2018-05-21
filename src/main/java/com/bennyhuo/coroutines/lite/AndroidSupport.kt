package com.bennyhuo.coroutines.lite

import android.os.Handler
import android.os.Looper

/**
 * Created by benny on 2018/5/20.
 */
object UIDispatcher: Dispatcher {
    private val handler = Handler(Looper.getMainLooper())

    override fun dispatch(block: () -> Unit) {
        handler.post(block)
    }
}

object UI: DispatcherContext(UIDispatcher)