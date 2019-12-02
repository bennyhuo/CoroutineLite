package com.bennyhuo.kotlin.coroutines.dispatcher.ui

import android.os.Handler
import android.os.Looper
import com.bennyhuo.kotlin.coroutines.dispatcher.Dispatcher


object AndroidDispatcher: Dispatcher {
    private val handler = Handler(Looper.getMainLooper())

    override fun dispatch(block: () -> Unit) {
        handler.post(block)
    }
}