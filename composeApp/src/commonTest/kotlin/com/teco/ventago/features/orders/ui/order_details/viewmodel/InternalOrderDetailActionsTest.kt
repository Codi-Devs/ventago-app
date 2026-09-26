package com.teco.ventago.features.orders.ui.order_details.viewModel

import com.teco.ventago.features.invoicing.domain.models.InvoiceStatus
import com.teco.ventago.features.orders.domain.models.ManualPaymentMethodOption
import com.teco.ventago.features.orders.domain.models.Order
import com.teco.ventago.features.orders.domain.models.OrderStatus
import com.teco.ventago.features.orders.domain.models.PaymentStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InternalOrderDetailActionsTest {

    @Test
    fun unpaidInternalOpenShowsRegisterPaymentAndFacturar() {
        val order = sampleInternalOrder(
            paymentStatus = PaymentStatus.UNPAID.id,
            invoiceStatus = InvoiceStatus.NONE.id,
            status = OrderStatus.DRAFT,
        )

        assertTrue(evaluateCanRegisterInternalPayment(order, canCreateNonFiscal = true))
        assertTrue(evaluateCanFacturarInternal(order, canMarkPaid = true))
        assertFalse(evaluateCanRegisterInternalPayment(order, canCreateNonFiscal = false))
    }

    @Test
    fun confirmedUninvoicedInternalAlsoShowsRegisterPayment() {
        val order = sampleInternalOrder(
            paymentStatus = PaymentStatus.PARTIAL.id,
            invoiceStatus = InvoiceStatus.NONE.id,
            status = OrderStatus.CONFIRMED,
        )

        assertTrue(evaluateCanRegisterInternalPayment(order, canCreateNonFiscal = true))
        assertTrue(evaluateCanFacturarInternal(order, canMarkPaid = true))
    }

    @Test
    fun paidInternalHidesRegisterPaymentAndKeepsFacturar() {
        val order = sampleInternalOrder(
            paymentStatus = PaymentStatus.PAID.id,
            invoiceStatus = InvoiceStatus.NONE.id,
            status = OrderStatus.CONFIRMED,
        )

        assertFalse(evaluateCanRegisterInternalPayment(order, canCreateNonFiscal = true))
        assertTrue(evaluateCanFacturarInternal(order, canMarkPaid = true))
        assertTrue(shouldInvoiceAfterInternalPayment(order, InvoiceStatus.NONE.id))
        assertFalse(shouldInvoiceAfterInternalPayment(order, InvoiceStatus.ISSUED.id))
    }

    @Test
    fun issuedInternalDoesNotShowInternalActions() {
        val order = sampleInternalOrder(
            paymentStatus = PaymentStatus.PARTIAL.id,
            invoiceStatus = InvoiceStatus.ISSUED.id,
            status = OrderStatus.CONFIRMED,
        )

        assertFalse(evaluateCanRegisterInternalPayment(order, canCreateNonFiscal = true))
        assertFalse(evaluateCanFacturarInternal(order, canMarkPaid = true))
        assertFalse(isInternalOpenOrder(order))
    }

    @Test
    fun implicitInvoiceDoesNotGetInternalPaymentAction() {
        val order = sampleInternalOrder(
            paymentStatus = PaymentStatus.UNPAID.id,
            invoiceStatus = InvoiceStatus.NONE.id,
            status = OrderStatus.DRAFT,
            invoicingMode = null,
        )

        assertFalse(isInternalOpenOrder(order))
        assertFalse(evaluateCanRegisterInternalPayment(order, canCreateNonFiscal = true))
        assertFalse(evaluateCanFacturarInternal(order, canMarkPaid = true))
    }

    @Test
    fun paymentOnlySheetAllowsPartialAmount() {
        val partial = ManualPaymentState(
            totalToChargeCents = 10_000L,
            charged = mapOf(ManualPaymentMethodOption.CASH.id to 2_500L),
            paymentOnly = true,
        )
        val fullRequired = partial.copy(paymentOnly = false)

        assertTrue(partial.isConfirmEnabled)
        assertFalse(fullRequired.isConfirmEnabled)
        assertEquals(7_500L, partial.remaining)
    }

    @Test
    fun confirmedInternalShowsPrintTicketOnPosOrConfiguredPrinter() {
        val order = sampleInternalOrder(
            paymentStatus = PaymentStatus.PAID.id,
            invoiceStatus = InvoiceStatus.NONE.id,
            status = OrderStatus.CONFIRMED,
            nonFiscalConfirmedAt = "2026-09-25T10:00:00-05:00",
        )

        assertTrue(order.hasCurrentNonFiscalDocument())
        assertTrue(
            evaluateCanShowReprintAction(
                order = order,
                hideReprintTicketAction = false,
                reprintInFlight = false,
                businessId = 4,
                hasLocalPrintCapability = true,
            )
        )
        assertFalse(
            evaluateCanShowReprintAction(
                order = order,
                hideReprintTicketAction = false,
                reprintInFlight = false,
                businessId = 4,
                hasLocalPrintCapability = false,
            )
        )
        assertTrue(
            evaluateCanShowReprintAction(
                order = order.copy(ticketEnabled = true),
                hideReprintTicketAction = false,
                reprintInFlight = false,
                businessId = 4,
                hasLocalPrintCapability = false,
            )
        )
    }

    @Test
    fun unconfirmedInternalDoesNotShowPrintTicket() {
        val order = sampleInternalOrder(
            paymentStatus = PaymentStatus.UNPAID.id,
            invoiceStatus = InvoiceStatus.NONE.id,
            status = OrderStatus.DRAFT,
        )

        assertFalse(order.hasCurrentNonFiscalDocument())
        assertFalse(
            evaluateCanShowReprintAction(
                order = order,
                hideReprintTicketAction = false,
                reprintInFlight = false,
                businessId = 4,
                hasLocalPrintCapability = true,
            )
        )
    }

    @Test
    fun issuedInvoiceShowsReprintWhenTicketEnabledOrLocalPrinterExists() {
        val order = sampleInternalOrder(
            paymentStatus = PaymentStatus.PAID.id,
            invoiceStatus = InvoiceStatus.ISSUED.id,
            status = OrderStatus.CONFIRMED,
            invoicingMode = null,
            ticketEnabled = true,
        )

        assertTrue(
            evaluateCanShowReprintAction(
                order = order,
                hideReprintTicketAction = false,
                reprintInFlight = false,
                businessId = 4,
                hasLocalPrintCapability = false,
            )
        )
        assertTrue(
            evaluateCanShowReprintAction(
                order = order.copy(ticketEnabled = false),
                hideReprintTicketAction = false,
                reprintInFlight = false,
                businessId = 4,
                hasLocalPrintCapability = true,
            )
        )
        assertFalse(
            evaluateCanShowReprintAction(
                order = order.copy(ticketEnabled = false),
                hideReprintTicketAction = false,
                reprintInFlight = false,
                businessId = 4,
                hasLocalPrintCapability = false,
            )
        )
    }

    private fun sampleInternalOrder(
        paymentStatus: Int,
        invoiceStatus: Int,
        status: Int,
        invoicingMode: String? = "explicit",
        nonFiscalConfirmedAt: String? = null,
        ticketEnabled: Boolean? = null,
    ): Order {
        return Order(
            id = 11524,
            orderType = "01",
            businessId = 4,
            internalNumber = "ORD-4-0000-001-0000011524",
            invoiceStatus = invoiceStatus,
            currencyCode = "USD",
            subtotal = "10.00",
            discountTotal = "0.00",
            taxableBase = "10.00",
            taxTotal = "0.00",
            tipsTotal = "0.00",
            totalAmount = "10.00",
            status = status,
            paymentStatus = paymentStatus,
            invoicingMode = invoicingMode,
            ticketEnabled = ticketEnabled,
            nonFiscalConfirmedAt = nonFiscalConfirmedAt,
            createdAt = "2026-09-25T10:00:00-05:00",
        )
    }
}
