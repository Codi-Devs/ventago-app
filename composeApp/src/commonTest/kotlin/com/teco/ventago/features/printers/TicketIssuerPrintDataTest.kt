package com.teco.ventago.features.printers

import com.teco.ventago.features.printers.domain.model.TicketDocumentPayload
import com.teco.ventago.json
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.decodeFromString

class TicketIssuerPrintDataTest {

    @Test
    fun detectsIssuerNameOnInlineCreateTicket() {
        val payload = json.decodeFromString<TicketDocumentPayload>(
            """
            {
              "kind": "invoice_ticket_layout",
              "blocks": [
                {
                  "section": "issuer",
                  "type": "key_value",
                  "values": [
                    {"key": "issuer_name", "label": "Razón social", "value": "Cafe Demo"}
                  ]
                }
              ]
            }
            """.trimIndent()
        )
        assertTrue(payload.hasIssuerPrintData())
    }

    @Test
    fun emptyIssuerFromCreateWithoutPhpIsIncomplete() {
        val payload = json.decodeFromString<TicketDocumentPayload>(
            """
            {
              "kind": "invoice_ticket_layout",
              "blocks": [
                {
                  "section": "issuer",
                  "type": "key_value",
                  "values": [
                    {"key": "issuer_name", "label": "Razón social", "value": ""},
                    {"key": "issuer_tax_id", "label": "RUC", "value": ""}
                  ]
                }
              ]
            }
            """.trimIndent()
        )
        assertFalse(payload.hasIssuerPrintData())
    }
}
