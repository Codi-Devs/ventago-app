package com.teco.ventago.features.orders

import com.teco.ventago.features.orders.domain.AchPaymentNormalizer
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AchPaymentNormalizerTest {

    @Test
    fun normalizesAliasFieldsAndTimeline() {
        val payload = buildJsonObject {
            put("id", 9951)
            put("payment_intent_id", "pi_alias_123")
            put("payment_status", "pending_review")
            put("amount", 35.5)
            put("currency", "USD")
            put("payment_reference", "TRX-001")
            put("customer", "Cliente Demo")
            put("destination_account", "****1234")
            put("proof_id", 777)
            put("risk_score", 42)
            put("timeline", buildJsonArray {
                add(buildJsonObject {
                    put("state", "created")
                    put("event", "Pago creado")
                    put("description", "Se creó el pago ACH")
                    put("timestamp", "2026-04-17T10:00:00-05:00")
                })
            })
        }

        val normalized = AchPaymentNormalizer.fromApiPayload(payload)

        assertEquals("9951", normalized.paymentId)
        assertEquals("pi_alias_123", normalized.paymentUid)
        assertEquals("pending_review", normalized.paymentStatus)
        assertEquals(35.5, normalized.amount)
        assertEquals("USD", normalized.currencyCode)
        assertEquals("TRX-001", normalized.reference)
        assertEquals("Cliente Demo", normalized.customerName)
        assertEquals("****1234", normalized.destinationAccount)
        assertEquals("777", normalized.proofId)
        assertEquals(42, normalized.riskScore)
        assertNotNull(normalized.timeline.firstOrNull())
        assertEquals("created", normalized.timeline.first().status)
        assertEquals("Pago creado", normalized.timeline.first().title)
    }

    @Test
    fun normalizesNestedApiPayloadWithUuidProofsAndEvents() {
        val payload = buildJsonObject {
            put("order_amount", "20.00")
            put("currency_code", "USD")
            put("order_external_uuid", "fceb421f-730e-4b2c-9a8e-9f4b589c0e5a")
            put("events", buildJsonArray {
                add(buildJsonObject {
                    put("event_type", "approved")
                    put("created_at", "2026-04-15T01:35:07Z")
                    put("payload", buildJsonObject {
                        put("notes", "")
                    })
                })
                add(buildJsonObject {
                    put("event_type", "checkout_created")
                    put("created_at", "2026-04-15T01:33:13Z")
                    put("payload", buildJsonObject {
                        put("reference_id", "ACH-ORD-36-7D8E6A")
                    })
                })
            })
            put("account", buildJsonObject {
                put("bank_name", "Banco General, S.A.")
                put("account_number_masked", "********6548")
                put("currency_code", "USD")
            })
            put("payment", buildJsonObject {
                put("id", "98cd6c70-045a-469e-babc-b5bd10dcf8cc")
                put("status", "approved")
                put("payer_name", "Oscar Hernandez")
                put("payer_email", "oegh1296@gmail.com")
                put("reference_code", "ACH-ORD-36-7D8E6A")
                put("risk_score", 100)
                put("risk_status", "high")
                put("decision_suggested", "reject_suspected")
                put("created_at", "2026-04-15T01:33:13Z")
            })
            put("latest_fraud", buildJsonObject {
                put("risk_band", "high")
                put("decision", "reject_suspected")
                put("final_score", 100)
                put("features", buildJsonObject {
                    put("expected_amount", "20.00")
                    put("detected_amount", "83.45")
                })
            })
            put("latest_ocr", buildJsonObject {
                put("detected_bank_name", "Banco General")
                put("detected_reference", "TRANSFERENCIA A TERCEROS")
            })
            put("proofs", buildJsonArray {
                add(buildJsonObject {
                    put("id", "e973d3ae-ddd7-4f92-8a1a-038f99dc2952")
                    put("is_primary", true)
                    put("mime_type", "application/pdf")
                })
            })
        }

        val normalized = AchPaymentNormalizer.fromApiPayload(payload)

        assertEquals("98cd6c70-045a-469e-babc-b5bd10dcf8cc", normalized.paymentId)
        assertEquals("98cd6c70-045a-469e-babc-b5bd10dcf8cc", normalized.paymentUid)
        assertEquals("approved", normalized.paymentStatus)
        assertEquals(20.0, normalized.amount)
        assertEquals("USD", normalized.currencyCode)
        assertEquals("ACH-ORD-36-7D8E6A", normalized.reference)
        assertEquals("Oscar Hernandez", normalized.customerName)
        assertEquals("oegh1296@gmail.com", normalized.customerEmail)
        assertEquals("Banco General, S.A.", normalized.bankName)
        assertEquals("********6548", normalized.destinationAccount)
        assertEquals("e973d3ae-ddd7-4f92-8a1a-038f99dc2952", normalized.proofId)
        assertEquals("application/pdf", normalized.proofContentType)
        assertTrue(normalized.proofFileName.orEmpty().endsWith(".pdf"))
        assertEquals(100, normalized.riskScore)
        assertEquals("high", normalized.riskLevel)
        assertEquals("reject_suspected", normalized.decisionSuggested)
        assertTrue(normalized.timeline.isNotEmpty())
        assertEquals("approved", normalized.timeline.first().status)
    }
}
