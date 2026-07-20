package com.teco.ventago.features.pos.ui

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class YappyOnsitePollingPolicyTest {

    @Test
    fun keepsPollingPendingAndSucceededInvoicePendingStates() {
        assertFalse(yappyOnsiteShouldStopPolling(status = "pending", invoiceStatus = 0))
        assertFalse(yappyOnsiteShouldStopPolling(status = "succeeded", invoiceStatus = 1))
    }

    @Test
    fun stopsPollingWhenInvoiceIsIssuedOrFailed() {
        assertTrue(yappyOnsiteShouldStopPolling(status = "succeeded", invoiceStatus = 2))
        assertTrue(yappyOnsiteShouldStopPolling(status = "succeeded", invoiceStatus = 3))
    }

    @Test
    fun stopsPollingForTerminalTransactionStates() {
        assertTrue(yappyOnsiteShouldStopPolling(status = "cancelled", invoiceStatus = 0))
        assertTrue(yappyOnsiteShouldStopPolling(status = "canceled", invoiceStatus = 0))
        assertTrue(yappyOnsiteShouldStopPolling(status = "expired", invoiceStatus = 0))
        assertTrue(yappyOnsiteShouldStopPolling(status = "returned", invoiceStatus = 0))
    }
}
