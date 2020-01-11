package com.bennyhuo.kotlin.coroutines.sample

import com.bennyhuo.kotlin.coroutines.delay
import com.bennyhuo.kotlin.coroutines.launch
import com.bennyhuo.kotlin.coroutines.scope.GlobalScope
import com.bennyhuo.kotlin.coroutines.utils.log

suspend fun main(){

   GlobalScope.launch {
       log(1)
       launch {
           delay(1000)
           log(4)
       }
       log(2)
       launch {
           delay(2000)
           log(5)
       }
       log(3)
   }.join()
    log(6)
}