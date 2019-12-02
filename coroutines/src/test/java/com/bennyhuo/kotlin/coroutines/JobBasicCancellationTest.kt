/*
 * Copyright 2016-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package com.bennyhuo.kotlin.coroutines

import org.junit.Test

/*
 * Basic checks that check that cancellation more or less works,
 * parent is not cancelled on child cancellation and launch {}, Job(), async {} and
 * CompletableDeferred behave properly
 */

@Suppress("DEPRECATION") // cancel(cause)
class JobBasicCancellationTest : TestBase() {

    @Test
    fun testJobCancelChild() = runTest {
        val parent = launch {
            expect(1)
            val child = launch {
                expect(2)
            }
            child.cancel()
            child.join()
            expect(3)
        }

        parent.join()
        finish(4)
    }

    @Test
    fun testAsyncCancelChild() = runTest {
        val parent = async {
            expect(1)
            val child = async {
                expect(2)
            }
            child.cancel()
            child.await()
            expect(3)
        }

        parent.await()
        finish(4)
    }

}
