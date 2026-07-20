package com.teco.ventago.features.orders

import com.teco.ventago.features.orders.domain.models.Order
import com.teco.ventago.features.orders.domain.models.requests.RetryInvoiceResponse
import com.teco.ventago.features.orders.domain.models.responses.CreateOrderResponse
import com.teco.ventago.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.decodeFromString

class CreateOrderResponseParsingTest {

    @Test
    fun createOrderResponseParsesSnakeCaseInvoiceWarningFields() {
        val payload = """
            {
              "id": 5992,
              "order_number": "ORD-4-0000-865-0000000594",
              "order_date": "2026-06-05T16:41:30.212815-05:00",
              "order_amount": "1.61",
              "tax_amount": "0.11",
              "payment_status": 2,
              "invoice_status": 3,
              "invoice_warning_code": "buyer_ruc_invalid",
              "invoice_warning_message": "No se pudo crear la factura porque el RUC es inválido."
            }
        """.trimIndent()

        val response = json.decodeFromString<CreateOrderResponse>(payload)

        assertEquals("buyer_ruc_invalid", response.invoiceWarningCode)
        assertEquals("No se pudo crear la factura porque el RUC es inválido.", response.invoiceWarningMessage)
    }

    @Test
    fun createOrderResponseParsesCamelCaseInvoiceWarningFields() {
        val payload = """
            {
              "id": 5992,
              "order_number": "ORD-4-0000-865-0000000594",
              "order_date": "2026-06-05T16:41:30.212815-05:00",
              "order_amount": "1.61",
              "tax_amount": "0.11",
              "payment_status": 2,
              "invoice_status": 3,
              "invoiceWarningCode": "INV_001",
              "invoiceWarningMessage": "Intermitencia temporal"
            }
        """.trimIndent()

        val response = json.decodeFromString<CreateOrderResponse>(payload)

        assertEquals("INV_001", response.invoiceWarningCode)
        assertEquals("Intermitencia temporal", response.invoiceWarningMessage)
    }

    @Test
    fun createOrderResponseParsesOnsitePayment() {
        val payload = """
            {
              "id": 456,
              "order_number": "ORD-123-0000-001-0000001001",
              "payment_flow_type": "in_place",
              "order_date": "2026-07-07T15:00:00Z",
              "order_amount": "107.00",
              "tax_amount": "7.00",
              "payment_status": 0,
              "invoice_status": 0,
              "onsite_payment": {
                "id": "VRDNF-93260766",
                "transaction_id": "VRDNF-93260766",
                "order_id": 456,
                "session_id": "9efe350f-226a-4f60-8527-a170a3317e8b",
                "qr_hash": "j5AHEj3kY8tI",
                "qr_type": "DYN",
                "status": "pending",
                "provider_status": "PENDING",
                "amount": "107.00",
                "currency": "USD",
                "expires_at": "2026-07-07T15:05:00Z"
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<CreateOrderResponse>(payload)

        assertEquals("in_place", response.paymentFlowType)
        assertEquals("VRDNF-93260766", response.onsitePayment?.transactionId)
        assertEquals("j5AHEj3kY8tI", response.onsitePayment?.qrHash)
    }

    @Test
    fun retryInvoiceResponseParsesSnakeAndCamelInvoiceWarningFields() {
        val snakePayload = """
            {
              "order_id": 55,
              "order_number": "ORD-55",
              "invoice_status": 1,
              "cufe": "",
              "invoice_id": "",
              "invoice_warning_code": "inv_001",
              "invoice_warning_message": "Pendiente"
            }
        """.trimIndent()
        val camelPayload = """
            {
              "order_id": 55,
              "order_number": "ORD-55",
              "invoice_status": 1,
              "cufe": "",
              "invoice_id": "",
              "invoiceWarningCode": "INV_001",
              "invoiceWarningMessage": "Pendiente camel"
            }
        """.trimIndent()

        val snake = json.decodeFromString<RetryInvoiceResponse>(snakePayload)
        val camel = json.decodeFromString<RetryInvoiceResponse>(camelPayload)

        assertEquals("inv_001", snake.invoiceWarningCode)
        assertEquals("Pendiente", snake.invoiceWarningMessage)
        assertEquals("INV_001", camel.invoiceWarningCode)
        assertEquals("Pendiente camel", camel.invoiceWarningMessage)
    }

    @Test
    fun orderParsesCamelCaseInvoiceWarningFields() {
        val payload = """
            {
              "id": 42,
              "order_type": "sale",
              "business_id": 4,
              "internal_number": "ORD-4-0000-865-0000000594",
              "external_invoice_number": null,
              "invoice_status": 3,
              "currency_code": "USD",
              "subtotal": "1.50",
              "discount_total": "0.00",
              "taxable_base": "1.50",
              "tax_total": "0.11",
              "tips_total": "0.00",
              "total_amount": "1.61",
              "status": 1,
              "payment_status": 2,
              "invoiceWarningCode": "buyer_ruc_invalid",
              "invoiceWarningMessage": "Factura inválida",
              "created_at": "2026-06-05T16:41:30.212815-05:00"
            }
        """.trimIndent()

        val order = json.decodeFromString<Order>(payload)

        assertEquals("buyer_ruc_invalid", order.invoiceWarningCode)
        assertEquals("Factura inválida", order.invoiceWarningMessage)
    }
}
