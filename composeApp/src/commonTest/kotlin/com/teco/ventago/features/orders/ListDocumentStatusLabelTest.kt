package com.teco.ventago.features.orders

import com.teco.ventago.features.invoicing.domain.models.InvoiceStatus
import com.teco.ventago.features.orders.domain.models.Order
import com.teco.ventago.features.orders.domain.models.OrderStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class ListDocumentStatusLabelTest {

    @Test
    fun voidedInvoiceShowsAnuladaEvenWhenOrderIsCancelled() {
        val order = order(
            status = OrderStatus.CANCELLED,
            invoiceStatus = InvoiceStatus.CANCELLED.id,
        )

        assertEquals("Anulada", order.listDocumentStatusLabel())
    }

    @Test
    fun cancelledOrderWithoutIssuedInvoiceShowsCancelado() {
        assertEquals(
            "Cancelado",
            order(status = OrderStatus.CANCELLED, invoiceStatus = InvoiceStatus.NONE.id)
                .listDocumentStatusLabel(),
        )
        assertEquals(
            "Cancelado",
            order(status = OrderStatus.CANCELLED, invoiceStatus = InvoiceStatus.PENDING.id)
                .listDocumentStatusLabel(),
        )
        assertEquals(
            "Cancelado",
            order(status = OrderStatus.CANCELLED, invoiceStatus = InvoiceStatus.FAILED.id)
                .listDocumentStatusLabel(),
        )
        assertEquals(
            "Cancelado",
            order(status = OrderStatus.CANCELLED, invoiceStatus = null)
                .listDocumentStatusLabel(),
        )
    }

    @Test
    fun issuedInvoiceStaysFacturadoWhenOrderIsCancelled() {
        val order = order(
            status = OrderStatus.CANCELLED,
            invoiceStatus = InvoiceStatus.ISSUED.id,
        )

        assertEquals("Facturado", order.listDocumentStatusLabel())
    }

    @Test
    fun openOrderKeepsExistingLabels() {
        assertEquals(
            "Fallida",
            order(status = OrderStatus.CONFIRMED, invoiceStatus = InvoiceStatus.FAILED.id)
                .listDocumentStatusLabel(),
        )
        assertEquals(
            "Borrador",
            order(status = OrderStatus.DRAFT, invoiceStatus = InvoiceStatus.NONE.id)
                .listDocumentStatusLabel(),
        )
        assertEquals(
            "Sin Factura",
            order(status = OrderStatus.CONFIRMED, invoiceStatus = InvoiceStatus.NONE.id)
                .listDocumentStatusLabel(),
        )
        assertEquals(
            "Sin Factura",
            order(status = OrderStatus.REJECT, invoiceStatus = InvoiceStatus.NONE.id)
                .listDocumentStatusLabel(),
        )
        assertEquals(
            "Sin Factura",
            order(status = OrderStatus.REFUNDED, invoiceStatus = InvoiceStatus.PENDING.id)
                .listDocumentStatusLabel(),
        )
    }

    private fun order(status: Int, invoiceStatus: Int?): Order {
        return Order(
            id = 1,
            orderType = "01",
            businessId = 10,
            internalNumber = "ORD-100",
            currencyCode = "USD",
            subtotal = "10.00",
            discountTotal = "0.00",
            taxableBase = "10.00",
            taxTotal = "0.00",
            tipsTotal = "0.00",
            totalAmount = "10.00",
            status = status,
            invoiceStatus = invoiceStatus,
            createdAt = "2026-10-22T09:00:00Z",
        )
    }
}
