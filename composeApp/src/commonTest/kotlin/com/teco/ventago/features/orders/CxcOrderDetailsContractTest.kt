package com.teco.ventago.features.orders

import com.teco.ventago.features.orders.domain.models.Order
import com.teco.ventago.features.orders.domain.models.RelatedDocument
import com.teco.ventago.features.orders.domain.models.requests.ManualPaymentItemRequest
import com.teco.ventago.features.orders.domain.models.requests.PaymentApplicationRequest
import com.teco.ventago.features.orders.domain.models.requests.PendingIntentCreateResponse
import com.teco.ventago.features.orders.domain.models.requests.PendingIntentReleaseResponse
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
import kotlinx.serialization.json.decodeFromJsonElement
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
    fun pendingIntentReleaseParsesNextActionsArray() {
        val payload = """
            {
              "released": true,
              "payment_method": "payment_link",
              "next_actions": ["manual_payment_allowed"],
              "order_id": 123,
              "order_number": "ORD-123",
              "payment_status": 1,
              "invoice_status": 0
            }
        """.trimIndent()

        val parsed = json.decodeFromString<PendingIntentReleaseResponse>(payload)

        assertTrue(parsed.released)
        assertEquals("manual_payment_allowed", parsed.nextAction)
        assertTrue(parsed.allowsNextAction("manual_payment_allowed"))
    }

    @Test
    fun pendingIntentCreateResolvesNestedPaymentLinkUrl() {
        val snakePayload = """
            {
              "payment_status": 1,
              "invoice_status": 0,
              "order_number": "ORD-123",
              "payment_link": {
                "payment_link_url": "https://pay.example/snake",
                "amount": "25.00"
              }
            }
        """.trimIndent()
        val camelPayload = """
            {
              "paymentLink": {
                "paymentLinkUrl": "https://pay.example/camel"
              }
            }
        """.trimIndent()

        val snake = json.decodeFromString<PendingIntentCreateResponse>(snakePayload)
        val camel = json.decodeFromString<PendingIntentCreateResponse>(camelPayload)

        assertEquals("https://pay.example/snake", snake.resolvedPaymentLinkUrl())
        assertEquals("https://pay.example/camel", camel.resolvedPaymentLinkUrl())
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

    @Test
    fun orderParsesRelatedDocumentsAndCreditCapacity() {
        val payload = """
            {
              "id": 7000,
              "order_type": "01",
              "business_id": 4,
              "internal_number": "ORD-4-0000-865-0000000700",
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
              "order_histories": [],
              "order_payments": [],
              "related_documents": [
                {
                  "order_id": 7001,
                  "order_number": "ORD-4-0000-865-0000000701",
                  "document_type": "04",
                  "relation_type": "credit_note",
                  "total_amount": "3.00",
                  "invoice_status": 1,
                  "external_invoice_number": "FE0120",
                  "emission_date": "2026-07-01T10:30:00-05:00"
                },
                {
                  "order_id": 7002,
                  "order_number": "ORD-4-0000-865-0000000702",
                  "document_type": "06",
                  "relation_type": "credit_note",
                  "total_amount": "4.00",
                  "invoice_status": 2
                },
                {
                  "order_id": 7003,
                  "order_number": "ORD-4-0000-865-0000000703",
                  "document_type": "04",
                  "relation_type": "credit_note",
                  "total_amount": "8.00",
                  "invoice_status": 3
                },
                {
                  "order_id": 7004,
                  "order_number": "ORD-4-0000-865-0000000704",
                  "document_type": "05",
                  "relation_type": "debit_note",
                  "total_amount": "2.00",
                  "invoice_status": 2
                }
              ],
              "created_at": "2026-07-01T10:00:00Z"
            }
        """.trimIndent()

        val parsed = json.decodeFromString(Order.serializer(), payload)

        assertEquals(4, parsed.relatedDocuments.size)
        assertEquals(7001L, parsed.relatedDocuments.first().orderId)
        assertEquals("ORD-4-0000-865-0000000701", parsed.relatedDocuments.first().orderNumber)
        assertEquals("04", parsed.relatedDocuments.first().documentType)
        assertEquals("credit_note", parsed.relatedDocuments.first().relationType)
        assertEquals("3.00", parsed.relatedDocuments.first().totalAmount)
        assertEquals(1, parsed.relatedDocuments.first().invoiceStatus)
        assertEquals("FE0120", parsed.relatedDocuments.first().externalInvoiceNumber)
        assertEquals("2026-07-01T10:30:00-05:00", parsed.relatedDocuments.first().emissionDate)
        assertEquals("Nota de crédito", parsed.relatedDocuments.first().displayType())
        assertEquals("Nota de débito", parsed.relatedDocuments.last().displayType())
        assertEquals(700L, parsed.activeCreditNoteTotalCents())
        assertEquals(300L, parsed.remainingCreditNoteCapacityCents())
    }

    @Test
    fun getOrdersResponseParsesRelatedDocumentsFromItems() {
        val payload = """
            {
              "success": true,
              "data": {
                "total": 816,
                "page": 1,
                "size": 10,
                "items": [
                  {
                    "id": 7776,
                    "order_type": "01",
                    "business_id": 4,
                    "internal_number": "ORD-4-0000-001-0000000112",
                    "payment_flow_type": "payment_link",
                    "external_invoice_number": "FE0120000155704849-2-2021-3200002026071500000001120010320663966835",
                    "external_invoice_id": "7600",
                    "invoice_status": 2,
                    "emission_date": "2026-07-15T11:00:00Z",
                    "currency_code": "USD",
                    "lines": [],
                    "subtotal": "43.21",
                    "discount_total": "0",
                    "taxable_base": "43.21",
                    "tax_total": "3.02",
                    "tips_total": "0",
                    "acarreo_total": "0",
                    "insurance_total": "0",
                    "other_charges_total": "0",
                    "total_amount": "46.23",
                    "status": 2,
                    "receiver_name": "",
                    "receiver_phone": "00000",
                    "order_histories": [],
                    "order_payments": [],
                    "related_documents": [
                      {
                        "order_id": 7801,
                        "order_number": "ORD-4-0000-001-0000000113",
                        "document_type": "04",
                        "relation_type": "credit_note",
                        "total_amount": "46.23",
                        "invoice_status": 2,
                        "external_invoice_number": "FE0420000155704849-2-2021-3200002026071600000001130010320663966835",
                        "emission_date": "2026-07-16T16:03:05Z"
                      }
                    ],
                    "created_at": "2026-07-15T11:00:00Z"
                  }
                ]
              },
              "error": null
            }
        """.trimIndent()

        val envelope = json.decodeFromString<JsonObject>(payload)
        val items = envelope.jsonObject["data"]!!.jsonObject["items"]!!.jsonArray
        val parsed = json.decodeFromJsonElement<Order>(items.first())
        val relatedDocument = parsed.relatedDocuments.single()

        assertEquals("ORD-4-0000-001-0000000113", relatedDocument.orderNumber)
        assertEquals("04", relatedDocument.documentType)
        assertEquals("04 - Nota de crédito", relatedDocument.displayOrderType())
        assertEquals("46.23", relatedDocument.totalAmount)
        assertEquals(4623L, parsed.activeCreditNoteTotalCents())
        assertEquals(0L, parsed.remainingCreditNoteCapacityCents())
    }

    @Test
    fun creditNoteDocumentTypesDoNotSupportReceivableActions() {
        val regularInvoice = sampleOrderForDocumentType("01")
        val referencedCreditNote = sampleOrderForDocumentType("04")
        val genericCreditNote = sampleOrderForDocumentType("06")
        val referencedDebitNote = sampleOrderForDocumentType("05")
        val genericDebitNote = sampleOrderForDocumentType("07")

        assertEquals(false, regularInvoice.isCreditNoteDocument())
        assertEquals(true, regularInvoice.supportsReceivableActions())
        assertEquals(true, referencedCreditNote.isCreditNoteDocument())
        assertEquals(false, referencedCreditNote.supportsReceivableActions())
        assertEquals(true, genericCreditNote.isCreditNoteDocument())
        assertEquals(false, genericCreditNote.supportsReceivableActions())
        assertEquals(false, referencedDebitNote.isCreditNoteDocument())
        assertEquals(true, referencedDebitNote.supportsReceivableActions())
        assertEquals(false, genericDebitNote.isCreditNoteDocument())
        assertEquals(true, genericDebitNote.supportsReceivableActions())
    }

    @Test
    fun orderDefaultsRelatedDocumentsAndClampsCreditCapacity() {
        val payload = """
            {
              "id": 7010,
              "order_type": "01",
              "business_id": 4,
              "internal_number": "ORD-4-0000-865-0000000710",
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
              "order_histories": [],
              "order_payments": [],
              "created_at": "2026-07-01T10:00:00Z"
            }
        """.trimIndent()

        val parsed = json.decodeFromString(Order.serializer(), payload)

        assertEquals(emptyList(), parsed.relatedDocuments)
        assertEquals(0L, parsed.activeCreditNoteTotalCents())
        assertEquals(1000L, parsed.remainingCreditNoteCapacityCents())

        val overCredited = parsed.copy(
            relatedDocuments = listOf(
                RelatedDocument(
                    orderId = 7011L,
                    orderNumber = "ORD-4-0000-865-0000000711",
                    documentType = "04",
                    relationType = "credit_note",
                    totalAmount = "11.00",
                    invoiceStatus = 2
                )
            )
        )

        assertEquals(1100L, overCredited.activeCreditNoteTotalCents())
        assertEquals(0L, overCredited.remainingCreditNoteCapacityCents())
    }

    private fun sampleOrderForDocumentType(orderType: String): Order {
        return Order(
            id = 7100,
            orderType = orderType,
            businessId = 4,
            internalNumber = "ORD-4-0000-865-0000000710",
            invoiceStatus = 2,
            currencyCode = "USD",
            subtotal = "10.00",
            discountTotal = "0.00",
            taxableBase = "10.00",
            taxTotal = "0.00",
            tipsTotal = "0.00",
            totalAmount = "10.00",
            status = 2,
            paymentStatus = 1,
            createdAt = "2026-07-01T10:00:00Z"
        )
    }
}
