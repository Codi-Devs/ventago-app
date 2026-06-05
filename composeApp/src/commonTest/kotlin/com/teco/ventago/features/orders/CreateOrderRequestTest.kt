package com.teco.ventago.features.orders

import com.teco.ventago.features.orders.domain.models.requests.Branch
import com.teco.ventago.features.orders.domain.models.requests.CreateOrderRequest
import com.teco.ventago.features.orders.domain.models.requests.CreateOrderTotals
import com.teco.ventago.features.orders.domain.models.requests.Invoice
import com.teco.ventago.json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class CreateOrderRequestTest {
    @Test
    fun createOrderSerializesIncludeBottomNoteTrue() {
        val payload = json.parseToJsonElement(
            json.encodeToString(sampleRequest(includeBottomNote = true))
        ).jsonObject

        assertEquals("true", payload["include_bottom_note"].toString())
    }

    @Test
    fun createOrderSerializesIncludeBottomNoteFalse() {
        val payload = json.parseToJsonElement(
            json.encodeToString(sampleRequest(includeBottomNote = false))
        ).jsonObject

        assertEquals("false", payload["include_bottom_note"].toString())
    }

    @Test
    fun createOrderSerializesIncludeBottomNoteNullForUnavailableConfig() {
        val payload = json.parseToJsonElement(
            json.encodeToString(sampleRequest(includeBottomNote = null))
        ).jsonObject

        assertEquals(JsonNull, payload["include_bottom_note"])
    }

    private fun sampleRequest(includeBottomNote: Boolean?): CreateOrderRequest {
        return CreateOrderRequest(
            invoice = Invoice(
                type = "01",
                operationNature = "01",
                operationDestination = "1",
            ),
            branch = Branch(code = "0000", billingPoint = "001"),
            totals = CreateOrderTotals(
                quantityItems = 1,
                subtotal = "10.00",
                totalBeforeDiscounts = "10.00",
                totalAfterDiscounts = "10.00",
                totalBeforeTaxes = "10.00",
                totalAfterTaxes = "10.00",
                totalTaxes = "0.00",
                invoiceTotal = "10.00",
            ),
            includeBottomNote = includeBottomNote,
        )
    }
}
