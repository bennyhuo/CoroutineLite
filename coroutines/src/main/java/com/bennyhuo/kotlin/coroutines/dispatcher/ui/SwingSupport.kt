package com.bennyhuo.kotlin.coroutines.dispatcher.ui

import com.bennyhuo.kotlin.coroutines.dispatcher.Dispatcher
import javax.swing.SwingUtilities

object SwingDispatcher: Dispatcher {

    override fun dispatch(block: () -> Unit) {
        SwingUtilities.invokeLater(block)
    }

}