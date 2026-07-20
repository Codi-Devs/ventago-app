package com.teco.ventago.features.payments

import com.teco.ventago.features.financialProfile.domain.model.PaymentSummary
import com.teco.ventago.features.payments.domain.models.YappyOnsiteDeviceConfigRequest
import com.teco.ventago.features.payments.domain.models.YappyOnsiteGroupConfigRequest
import com.teco.ventago.features.payments.domain.models.YappyOnsiteTransactionPayload
import com.teco.ventago.features.payments.domain.models.YappyOnsiteTransactionStatus
import com.teco.ventago.json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class YappyOnsiteContractsTest {
    @Test
    fun paymentSummaryParsesYappyOnsiteAggregate() {
        val payload = """
            {
              "onboarding_completed": true,
              "payment_methods": {
                "yappy": {
                  "visible": true,
                  "linked_account": false,
                  "onsite": {
                    "configured": true,
                    "enabled": true,
                    "groups_count": 1,
                    "devices_count": 2,
                    "open_sessions_count": 1,
                    "has_open_session": true
                  }
                }
              }
            }
        """.trimIndent()

        val summary = json.decodeFromString<PaymentSummary>(payload)

        assertEquals(true, summary.paymentMethods.yappy.onsite.configured)
        assertEquals(2, summary.paymentMethods.yappy.onsite.devicesCount)
        assertEquals(true, summary.paymentMethods.yappy.onsite.hasOpenSession)
    }

    @Test
    fun groupAndDeviceRequestsSerializeSnakeCaseFields() {
        val group = json.parseToJsonElement(
            json.encodeToString(
                YappyOnsiteGroupConfigRequest(
                    name = "Sucursal principal",
                    apiKey = "api",
                    secretKey = "secret",
                    branchCode = "001",
                )
            )
        ).jsonObject
        val device = json.parseToJsonElement(
            json.encodeToString(
                YappyOnsiteDeviceConfigRequest(
                    deviceId = "DEVICE-ID",
                    name = "Caja 1",
                    userCode = "CAJERO-1",
                    billingPoint = "002",
                )
            )
        ).jsonObject

        assertEquals("\"api\"", group["api_key"].toString())
        assertEquals("\"secret\"", group["secret_key"].toString())
        assertEquals("\"001\"", group["branch_code"].toString())
        assertEquals("\"DEVICE-ID\"", device["device_id"].toString())
        assertEquals("\"CAJERO-1\"", device["user_code"].toString())
        assertEquals("\"002\"", device["billing_point"].toString())
    }

    @Test
    fun transactionPayloadParsesPendingPayment() {
        val payload = json.decodeFromString<YappyOnsiteTransactionPayload>(basePayload("pending", invoiceStatus = 0))

        assertEquals("pending", payload.transaction.status)
        assertEquals(0, payload.order.paymentStatus)
        assertEquals(0, payload.invoice.status)
    }

    @Test
    fun transactionPayloadParsesPaidInvoicePending() {
        val payload = json.decodeFromString<YappyOnsiteTransactionPayload>(basePayload("succeeded", invoiceStatus = 1))

        assertEquals("succeeded", payload.transaction.status)
        assertEquals(1, payload.invoice.status)
    }

    @Test
    fun transactionPayloadParsesIssuedInvoiceWithTicket() {
        val payload = json.decodeFromString<YappyOnsiteTransactionPayload>(
            basePayload(
                status = "succeeded",
                invoiceStatus = 2,
                ticket = """, "ticket": { "ticket_layout": { "blocks": [] } }"""
            )
        )

        assertEquals(2, payload.invoice.status)
        assertNotNull(payload.invoice.ticket)
    }

    @Test
    fun transactionPayloadParsesIssuedInvoiceWithoutTicket() {
        val payload = json.decodeFromString<YappyOnsiteTransactionPayload>(basePayload("succeeded", invoiceStatus = 2))

        assertEquals(2, payload.invoice.status)
        assertNull(payload.invoice.ticket)
    }

    @Test
    fun transactionPayloadParsesInvoiceFailedWarning() {
        val payload = json.decodeFromString<YappyOnsiteTransactionPayload>(
            basePayload(
                status = "succeeded",
                invoiceStatus = 3,
                warning = """, "warning_code": "validation_error", "warning_message": "No se pudo crear la factura.""""
            )
        )

        assertEquals(3, payload.invoice.status)
        assertEquals("No se pudo crear la factura.", payload.invoice.warningMessage)
    }

    @Test
    fun transactionStatusParsesTerminalCancelReturnStates() {
        val expired = json.decodeFromString<YappyOnsiteTransactionPayload>(basePayload("expired", invoiceStatus = 0))
        val cancelled = json.decodeFromString<YappyOnsiteTransactionStatus>(terminalTransaction("cancelled"))
        val returned = json.decodeFromString<YappyOnsiteTransactionStatus>(terminalTransaction("returned"))

        assertEquals("expired", expired.transaction.status)
        assertEquals("cancelled", cancelled.status)
        assertEquals("returned", returned.status)
    }

    private fun basePayload(
        status: String,
        invoiceStatus: Int,
        ticket: String = """, "ticket": null""",
        warning: String = """, "warning_code": null, "warning_message": null""",
    ): String {
        return """
            {
              "transaction": {
                "id": 31,
                "transaction_id": "VRDNF-93260766",
                "session_id": "9efe350f-226a-4f60-8527-a170a3317e8b",
                "status": "$status",
                "provider_status": "PENDING",
                "qr_type": "DYN",
                "qr_hash": "j5AHEj3kY8tI",
                "amount": "107.00",
                "currency": "USD",
                "expires_at": "2026-07-07T15:05:00Z"
              },
              "order": {
                "id": 456,
                "order_number": "ORD-123-0000-001-0000001001",
                "payment_flow_type": "in_place",
                "order_status": 2,
                "payment_status": 0,
                "invoice_status": $invoiceStatus,
                "total_amount": "107.00",
                "currency": "USD"
              },
              "invoice": {
                "status": $invoiceStatus,
                "invoice_id": "789",
                "cufe": "FE012345",
                "document_guid": "abc-123",
                "auth_number": "123456789",
                "invoiced_at": "2026-07-07T15:02:15Z",
                "ticket_enabled": true
                $ticket
                $warning
              }
            }
        """.trimIndent()
    }

    private fun terminalTransaction(status: String): String {
        return """
            {
              "id": 1,
              "business_id": 123,
              "order_id": 456,
              "session_id": "9efe350f-226a-4f60-8527-a170a3317e8b",
              "transaction_id": "VRDNF-93260766",
              "qr_type": "DYN",
              "status": "$status",
              "provider_status": "${status.uppercase()}",
              "amount": "107.00",
              "currency": "USD"
            }
        """.trimIndent()
    }
}
