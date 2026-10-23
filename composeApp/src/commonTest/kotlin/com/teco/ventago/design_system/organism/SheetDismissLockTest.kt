package com.teco.ventago.design_system.organism

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SheetDismissLockTest {

    @Test
    fun secondDismissOfTheSamePhaseIsIgnored() {
        val lock = SheetDismissLock()
        lock.sync(LoadingState.SUCCESS)

        assertTrue(lock.tryAcquire())
        assertFalse(lock.tryAcquire())
    }

    @Test
    fun aNewPhaseCanDismissAgain() {
        val lock = SheetDismissLock()
        lock.sync(LoadingState.SUCCESS)
        assertTrue(lock.tryAcquire())

        lock.sync(LoadingState.LOADING)
        lock.sync(LoadingState.ERROR)

        assertTrue(lock.tryAcquire())
        assertFalse(lock.tryAcquire())
    }
}
