package com.teco.ventago.features.printers

import com.teco.ventago.features.printers.domain.TicketPrinterChoice
import com.teco.ventago.features.printers.domain.chooseTicketPrinter
import kotlin.test.Test
import kotlin.test.assertEquals

class TicketPrinterRoutingTest {
    @Test
    fun activeBluetoothPrintsOnThisDeviceEvenWithoutBillingPoint() {
        assertEquals(
            TicketPrinterChoice.Bluetooth,
            chooseTicketPrinter(
                bluetoothActive = true,
                branchCode = null,
                billingPointCode = null,
                networkPrinterAvailable = true,
            ),
        )
    }

    @Test
    fun networkPrinterIsUsedOnlyWhenBluetoothIsInactive() {
        assertEquals(
            TicketPrinterChoice.Network,
            chooseTicketPrinter(
                bluetoothActive = false,
                branchCode = "001",
                billingPointCode = "001",
                networkPrinterAvailable = true,
            ),
        )
    }

    @Test
    fun missingBranchOrBillingPointDoesNotFallBackToNetwork() {
        assertEquals(
            TicketPrinterChoice.None,
            chooseTicketPrinter(
                bluetoothActive = false,
                branchCode = null,
                billingPointCode = "001",
                networkPrinterAvailable = true,
            ),
        )
        assertEquals(
            TicketPrinterChoice.None,
            chooseTicketPrinter(
                bluetoothActive = false,
                branchCode = "001",
                billingPointCode = " ",
                networkPrinterAvailable = true,
            ),
        )
    }
}
