package com.teco.ventago.features.orders

import com.teco.ventago.features.orders.domain.models.Order
import com.teco.ventago.features.orders.domain.models.requests.ManualPaymentItemRequest
import com.teco.ventago.features.orders.domain.models.requests.PaymentApplicationRequest
import com.teco.ventago.features.orders.domain.models.requests.RegisterManualPaymentsRequest
import com.teco.ventago.features.orders.domain.models.requests.RescheduleReceivableTermRequest
import com.teco.ventago.features.orders.domain.models.requests.RescheduleReceivablesRequest
import com.teco.ventago.features.orders.domain.models.requests.VoidOrderPaymentRequest
import com.teco.ventago.utils.ApiError
import com.teco.ventago.utils.ApiResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.decodeFromString

class CxcOrderDetailsContractTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Test
    fun orderParsesReceivableTermsAndVoidedPaymentFields() {
        val payload = """
            {
              "id": 2667,
              "order_type": "01",
              "business_id": 1,
              "internal_number": "001-001-00001234",
              "external_invoice_number": "CUFE-TEST",
              "invoice_status": 2,
              "currency_code": "USD",
              "lines": [],
              "subtotal": "100.00",
              "discount_total": "0.00",
              "taxable_base": "100.00",
              "tax_total": "7.00",
              "tips_total": "0.00",
              "total_amount": "107.00",
              "status": 5,
              "payment_status": 1,
              "order_histories": [],
              "order_payments": [
                {
                  "id": 2130,
                  "payment_intent_id": "pi_test",
                  "payment_method_id": { "ID": 2, "Name": "Efectivo", "Description": "Cash" },
                  "payment_status_str": "paid",
                  "payment_date": "2026-03-27T11:00:00-05:00",
                  "voided_at": "2026-03-27T12:00:00-05:00",
                  "void_reason": "Pago mal digitado",
                  "total_amount": "50.00",
                  "charged": "50.00",
                  "refunded": "0.00"
                }
              ],
              "receivable_terms": [
                {
                  "id": 9001,
                  "source_order_term_id": null,
                  "term_number": 1,
                  "due_date": 1775001600,
                  "original_amount": "100.00",
                  "open_amount": "75.00",
                  "status": 1,
                  "term_kind": "declared",
                  "notes": null
                }
              ],
              "created_at": "2026-03-27T12:00:00"
            }
        """.trimIndent()

        val parsed = json.decodeFromString(Order.serializer(), payload)
        assertEquals(1, parsed.receivableTerms.size)
        assertEquals(9001L, parsed.receivableTerms.first().id)
        assertEquals(2130L, parsed.orderPayments.first().id)
        assertEquals("Pago mal digitado", parsed.orderPayments.first().voidReason)
    }

    @Test
    fun orderParsesReceivableTermWhenStatusIsMissing() {
        val payload = """
            {
              "id": 2668,
              "order_type": "01",
              "business_id": 1,
              "internal_number": "001-001-00001235",
              "invoice_status": 2,
              "currency_code": "USD",
              "lines": [],
              "subtotal": "100.00",
              "discount_total": "0.00",
              "taxable_base": "100.00",
              "tax_total": "7.00",
              "tips_total": "0.00",
              "total_amount": "107.00",
              "status": 5,
              "payment_status": 1,
              "order_histories": [],
              "order_payments": [],
              "receivable_terms": [
                {
                  "id": 9101,
                  "term_number": 1,
                  "due_date": 1775001600,
                  "original_amount": "100.00",
                  "open_amount": "100.00",
                  "term_kind": "declared",
                  "notes": null
                }
              ],
              "created_at": "2026-03-27T12:00:00"
            }
        """.trimIndent()

        val parsed = json.decodeFromString(Order.serializer(), payload)
        assertEquals(1, parsed.receivableTerms.size)
        assertEquals(9101L, parsed.receivableTerms.first().id)
        assertEquals(0, parsed.receivableTerms.first().status)
    }

    @Test
    fun orderParsesHistoryWhenStatusIdIsMissing() {
        val payload = """
            {
              "id": 2669,
              "order_type": "01",
              "business_id": 1,
              "internal_number": "001-001-00001236",
              "invoice_status": 2,
              "currency_code": "USD",
              "lines": [],
              "subtotal": "100.00",
              "discount_total": "0.00",
              "taxable_base": "100.00",
              "tax_total": "7.00",
              "tips_total": "0.00",
              "total_amount": "107.00",
              "status": 5,
              "payment_status": 1,
              "order_histories": [
                {
                  "note": "Creada",
                  "changed_by": "system",
                  "created_at": "2026-03-27T12:00:00-05:00"
                }
              ],
              "order_payments": [],
              "receivable_terms": [],
              "created_at": "2026-03-27T12:00:00"
            }
        """.trimIndent()

        val parsed = json.decodeFromString(Order.serializer(), payload)
        assertEquals(1, parsed.orderHistories.size)
        assertEquals(0, parsed.orderHistories.first().statusId)
        assertEquals("Creada", parsed.orderHistories.first().note)
    }

    @Test
    fun finalConsumerOrderPrefersNonBlankReceiverName() {
        val payload = """
            {
              "id": 5980,
              "order_type": "01",
              "business_id": 4,
              "internal_number": "ORD-4-0000-865-0000000587",
              "invoice_status": 2,
              "currency_code": "USD",
              "lines": [],
              "subtotal": "1.50",
              "discount_total": "0.00",
              "taxable_base": "1.50",
              "tax_total": "0.11",
              "tips_total": "0.00",
              "total_amount": "1.61",
              "status": 2,
              "payment_status": 2,
              "receiver_name": "EMPRESA PRUEBA",
              "receiver_phone": "00000",
              "order_histories": [],
              "order_payments": [],
              "customer": {
                "id": 15,
                "name": "CONSUMIDOR FINAL",
                "email": "cf@pos.com",
                "phone": "15",
                "fe_customer_type": "02",
                "status": 1
              },
              "created_at": "2026-06-05T20:45:10Z"
            }
        """.trimIndent()

        val parsed = json.decodeFromString(Order.serializer(), payload)

        assertEquals("EMPRESA PRUEBA", parsed.displayCustomerName())
        assertEquals("00000", parsed.displayCustomerPhone())
        assertEquals("EMPRESA PRUEBA", parsed.displayCustomerSnapshot()?.name)
    }

    @Test
    fun nonFinalConsumerOrderKeepsCustomerNameWhenReceiverNameExists() {
        val payload = """
            {
              "id": 5981,
              "order_type": "01",
              "business_id": 4,
              "internal_number": "ORD-4-0000-865-0000000588",
              "invoice_status": 2,
              "currency_code": "USD",
              "lines": [],
              "subtotal": "10.00",
              "discount_total": "0.00",
              "taxable_base": "10.00",
              "tax_total": "0.00",
              "tips_total": "0.00",
              "total_amount": "10.00",
              "status": 2,
              "payment_status": 2,
              "receiver_name": "EMPRESA PRUEBA",
              "order_histories": [],
              "order_payments": [],
              "customer": {
                "id": 16,
                "name": "Cliente registrado",
                "phone": "61234567",
                "fe_customer_type": "01",
                "status": 1
              },
              "created_at": "2026-06-05T20:45:10Z"
            }
        """.trimIndent()

        val parsed = json.decodeFromString(Order.serializer(), payload)

        assertEquals("Cliente registrado", parsed.displayCustomerName())
        assertEquals("61234567", parsed.displayCustomerPhone())
    }

    @Test
    fun displayCustomerPhoneHidesPlaceholderPhoneValues() {
        val customerPhonePayload = """
            {
              "id": 5982,
              "order_type": "01",
              "business_id": 4,
              "internal_number": "ORD-4-0000-865-0000000589",
              "invoice_status": 2,
              "currency_code": "USD",
              "lines": [],
              "subtotal": "10.00",
              "discount_total": "0.00",
              "taxable_base": "10.00",
              "tax_total": "0.00",
              "tips_total": "0.00",
              "total_amount": "10.00",
              "status": 2,
              "payment_status": 2,
              "receiver_phone": "7777",
              "order_histories": [],
              "order_payments": [],
              "customer": {
                "id": 17,
                "name": "Cliente registrado",
                "phone": "0000",
                "fe_customer_type": "01",
                "status": 1
              },
              "created_at": "2026-06-05T20:45:10Z"
            }
        """.trimIndent()
        val receiverPhonePayload = """
            {
              "id": 5983,
              "order_type": "01",
              "business_id": 4,
              "internal_number": "ORD-4-0000-865-0000000590",
              "invoice_status": 2,
              "currency_code": "USD",
              "lines": [],
              "subtotal": "10.00",
              "discount_total": "0.00",
              "taxable_base": "10.00",
              "tax_total": "0.00",
              "tips_total": "0.00",
              "total_amount": "10.00",
              "status": 2,
              "payment_status": 2,
              "receiver_phone": "0000",
              "order_histories": [],
              "order_payments": [],
              "customer": {
                "id": 18,
                "name": "Cliente final",
                "phone": "",
                "fe_customer_type": "02",
                "status": 1
              },
              "created_at": "2026-06-05T20:45:10Z"
            }
        """.trimIndent()

        val customerPhoneParsed = json.decodeFromString(Order.serializer(), customerPhonePayload)
        val receiverPhoneParsed = json.decodeFromString(Order.serializer(), receiverPhonePayload)

        assertEquals("7777", customerPhoneParsed.displayCustomerPhone())
        assertNull(receiverPhoneParsed.displayCustomerPhone())
    }

    @Test
    fun registerManualPaymentsAutomaticPayloadOmitsApplications() {
        val request = RegisterManualPaymentsRequest(
            payments = listOf(
                ManualPaymentItemRequest(
                    type = 2,
                    amount = "150.00",
                    paymentDate = "2026-03-27T11:00:00-05:00"
                )
            )
        )

        val body = json.encodeToJsonElement(RegisterManualPaymentsRequest.serializer(), request).jsonObject
        val firstPayment = body["payments"]!!.jsonArray.first().jsonObject

        assertEquals("\"2026-03-27T11:00:00-05:00\"", firstPayment["payment_date"]?.toString())
        assertTrue(firstPayment["applications"] == null || firstPayment["applications"]?.toString() == "null")
    }

    @Test
    fun registerManualPaymentsManualPayloadIncludesApplications() {
        val request = RegisterManualPaymentsRequest(
            payments = listOf(
                ManualPaymentItemRequest(
                    type = 1,
                    amount = "30.00",
                    paymentDate = "2026-03-27T09:15:00-05:00",
                    applications = listOf(
                        PaymentApplicationRequest(
                            receivableTermId = 9001L,
                            amount = "30.00"
                        )
                    )
                )
            )
        )

        val body = json.encodeToJsonElement(RegisterManualPaymentsRequest.serializer(), request).jsonObject
        val firstPayment = body["payments"]!!.jsonArray.first().jsonObject

        val applications = firstPayment["applications"]!!.jsonArray
        assertEquals(1, applications.size)
        assertEquals("9001", applications.first().jsonObject["receivable_term_id"]?.toString())
        assertEquals("\"30.00\"", applications.first().jsonObject["amount"]?.toString())
    }

    @Test
    fun reschedulePayloadKeepsSourceIdsAndEmptyNotes() {
        val request = RescheduleReceivablesRequest(
            sourceTermIds = listOf(9001L, 9002L),
            newTerms = listOf(
                RescheduleReceivableTermRequest(
                    dueDate = "2026-04-15T00:00:00-05:00",
                    amount = "100.00",
                    notes = ""
                )
            ),
            note = ""
        )

        val body = json.encodeToJsonElement(RescheduleReceivablesRequest.serializer(), request).jsonObject
        assertEquals("[9001,9002]", body["source_term_ids"]?.toString())
        assertEquals("\"\"", body["note"]?.toString())
        assertEquals("\"\"", body["new_terms"]!!.jsonArray.first().jsonObject["notes"]?.toString())
    }

    @Test
    fun voidPayloadSerializesReason() {
        val request = VoidOrderPaymentRequest(reason = "Pago mal digitado")
        val body = json.encodeToJsonElement(VoidOrderPaymentRequest.serializer(), request).jsonObject
        assertEquals("\"Pago mal digitado\"", body["reason"]?.toString())
    }

    @Test
    fun apiResponseMapsOrp002Code() {
        val envelope = json.decodeFromString<JsonObject>(
            """
                {
                  "success": false,
                  "data": null,
                  "error": "O_RP_002"
                }
            """.trimIndent()
        )

        val response = ApiResponse.fromJson(envelope)
        assertEquals("O_RP_002", response.errorCode)
        assertEquals(ApiError.O_RP_002, response.error)
    }
}
