package com.teco.ventago.features.orders

import com.teco.ventago.features.orders.domain.models.requests.ManualPaymentItemRequest
import com.teco.ventago.features.orders.domain.models.requests.RegisterManualPaymentsDataResponse
import com.teco.ventago.features.orders.domain.models.requests.RegisterManualPaymentsRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RegisterManualPaymentsRequestTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun serializesCreditPaymentDueDate() {
        val request = RegisterManualPaymentsRequest(
            payments = listOf(
                ManualPaymentItemRequest(
                    type = 11,
                    amount = "25.00",
                    paymentDate = "2026-07-17T10:30:00-05:00",
                    dueDate = "2026-07-18T00:00:00-05:00",
                )
            )
        )

        val encoded = json.encodeToString(request)

        assertTrue(encoded.contains(""""due_date":"2026-07-18T00:00:00-05:00""""))
    }

    @Test
    fun parsesOptionalManualRegistrationPayloads() {
        val decoded = json.decodeFromString<RegisterManualPaymentsDataResponse>(
            """
            {
              "order_id": 123,
              "order_number": "F-001",
              "payment_status": 2,
              "invoice_status": 2,
              "invoiced": true,
              "invoice": { "status": 2, "cufe": "CUFE-123" },
              "order": { "id": 123 },
              "ticket": { "ticket_layout": { "blocks": [] } }
            }
            """.trimIndent()
        )

        assertEquals(123, decoded.orderId)
        assertEquals(2, decoded.invoiceStatus)
        assertNotNull(decoded.invoice)
        assertNotNull(decoded.order)
        assertNotNull(decoded.ticket?.resolvedLayoutElement())
    }

    @Test
    fun resolvesTicketFromNestedInvoicePayload() {
        val decoded = json.decodeFromString<RegisterManualPaymentsDataResponse>(
            """
            {
              "order_id": 123,
              "order_number": "F-001",
              "payment_status": 2,
              "invoice_status": 2,
              "invoiced": true,
              "invoice": {
                "status": 2,
                "ticket": { "ticket_layout": { "blocks": [] } }
              }
            }
            """.trimIndent()
        )

        assertNotNull(decoded.resolvedTicketPayload(json)?.resolvedLayoutElement())
    }

    @Test
    fun resolvesIssuedStatusFromManualRegistrationPayloadWhenFreshOrderIsStale() {
        val decoded = json.decodeFromString<RegisterManualPaymentsDataResponse>(
            """
            {
              "order_id": 123,
              "order_number": "F-001",
              "payment_status": 2,
              "invoice_status": 0,
              "invoiced": true,
              "invoice": {
                "status": 2,
                "cufe": "FE0120000155704849"
              },
              "order": {
                "invoice_status": 0
              }
            }
            """.trimIndent()
        )

        assertEquals(2, decoded.resolvedInvoiceStatus(freshOrderInvoiceStatus = 0))
        assertEquals("FE0120000155704849", decoded.resolvedInvoiceCufe(freshOrderExternalInvoiceNumber = null))
    }

    @Test
    fun resolvedFreshOrderCufeTakesPrecedenceOverNestedPayload() {
        val decoded = json.decodeFromString<RegisterManualPaymentsDataResponse>(
            """
            {
              "order_id": 123,
              "order_number": "F-001",
              "payment_status": 2,
              "invoice_status": 2,
              "invoiced": true,
              "invoice": {
                "status": 2,
                "cufe": "NESTED-CUFE"
              }
            }
            """.trimIndent()
        )

        assertEquals("FRESH-CUFE", decoded.resolvedInvoiceCufe(freshOrderExternalInvoiceNumber = "FRESH-CUFE"))
    }
}
