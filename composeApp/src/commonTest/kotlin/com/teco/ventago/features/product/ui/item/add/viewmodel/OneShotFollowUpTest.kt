package com.teco.ventago.features.product.ui.item.add.viewmodel

import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame

class OneShotFollowUpTest {

    @Test
    fun consumeReturnsTheArmedActionOnlyOnce() {
        val followUp = OneShotFollowUp()
        val action: suspend () -> Unit = {}
        followUp.arm(action)

        assertSame(action, followUp.consume())
        assertNull(followUp.consume())
    }

    @Test
    fun consumeWithoutArmReturnsNull() {
        assertNull(OneShotFollowUp().consume())
    }

    @Test
    fun armReplacesThePreviousAction() {
        val followUp = OneShotFollowUp()
        val first: suspend () -> Unit = {}
        val second: suspend () -> Unit = {}
        followUp.arm(first)
        followUp.arm(second)

        assertSame(second, followUp.consume())
        assertNull(followUp.consume())
    }
}
