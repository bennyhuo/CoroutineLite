package com.bennyhuo.kotlin.coroutines.dispatcher

import com.bennyhuo.kotlin.coroutines.dispatcher.ui.AndroidDispatcher
import com.bennyhuo.kotlin.coroutines.dispatcher.ui.SwingDispatcher

object Dispatchers {

    val Android by lazy {
        DispatcherContext(AndroidDispatcher)
    }

    val Swing by lazy {
        DispatcherContext(SwingDispatcher)
    }

    val Default by lazy {
        DispatcherContext(DefaultDispatcher)
    }
}