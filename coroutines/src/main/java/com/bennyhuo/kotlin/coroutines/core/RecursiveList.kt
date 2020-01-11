package com.bennyhuo.kotlin.coroutines.core

sealed class RecursiveList<out T> {
    object Nil: RecursiveList<Nothing>()
    class Cons<T>(val head: T, val tail: RecursiveList<T>): RecursiveList<T>()
}

fun <T> RecursiveList<T>.remove(element: T): RecursiveList<T> {
    return when(this){
        RecursiveList.Nil -> this
        is RecursiveList.Cons -> {
            if(head == element){
                return tail
            } else {
                RecursiveList.Cons(head, tail.remove(element))
            }
        }
    }
}

tailrec fun <T> RecursiveList<T>.forEach(action: (T) -> Unit): Unit = when(this){
    RecursiveList.Nil ->Unit
    is RecursiveList.Cons -> {
        action(this.head)
        this.tail.forEach(action)
    }
}

inline fun <reified T> RecursiveList<Any>.loopOn(crossinline action: (T) -> Unit) = forEach {
    when(it){
        is T -> action(it)
    }
}