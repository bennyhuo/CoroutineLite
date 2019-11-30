package com.bennyhuo.coroutines.lite

import org.junit.Assert
import java.util.concurrent.atomic.AtomicInteger

open class TestBase {

    private var step = AtomicInteger(0)

    fun expectStep(value: Int) {
        val expectStep = step.getAndIncrement()
        if (expectStep != value)
            throw StepException(expectStep, value)
    }

    infix fun Any?.shouldBe(value: Any?){
        Assert.assertEquals(value, this)
    }

}


class StepException(expect: Int, fact: Int) : Exception("Expect ${expect}, but got ${fact}")