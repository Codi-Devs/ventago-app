package com.teco.ventago.features.orders

import com.teco.ventago.features.orders.domain.models.requests.Branch
import com.teco.ventago.features.orders.domain.models.requests.CreateOrderRequest
import com.teco.ventago.features.orders.domain.models.requests.CreateOrderTotals
import com.teco.ventago.features.orders.domain.models.requests.FinalCustomerInfo
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

    @Test
    fun createOrderSerializesFinalForeignCustomerCountryCode() {
        val payload = json.parseToJsonElement(
            json.encodeToString(
                sampleRequest(
                    includeBottomNote = null,
                    finalCustomerInfo = FinalCustomerInfo(
                        name = "Prueba",
                        identificationType = "foreing_taxid",
                        identificationNumber = "1231321312",
                        countryCode = "AD"
                    )
                )
            )
        ).jsonObject
        val finalCustomerInfo = payload["final_customer_info"]!!.jsonObject

        assertEquals("\"Prueba\"", finalCustomerInfo["name"].toString())
        assertEquals("\"foreing_taxid\"", finalCustomerInfo["identification_type"].toString())
        assertEquals("\"1231321312\"", finalCustomerInfo["identification_number"].toString())
        assertEquals("\"AD\"", finalCustomerInfo["country_code"].toString())
    }

    @Test
    fun createOrderSerializesFinalPassportCustomerCountryCode() {
        val payload = json.parseToJsonElement(
            json.encodeToString(
                sampleRequest(
                    includeBottomNote = null,
                    finalCustomerInfo = FinalCustomerInfo(
                        name = "John Smith",
                        identificationType = "passport",
                        identificationNumber = "US123456789",
                        countryCode = "US"
                    )
                )
            )
        ).jsonObject
        val finalCustomerInfo = payload["final_customer_info"]!!.jsonObject

        assertEquals("\"John Smith\"", finalCustomerInfo["name"].toString())
        assertEquals("\"passport\"", finalCustomerInfo["identification_type"].toString())
        assertEquals("\"US123456789\"", finalCustomerInfo["identification_number"].toString())
        assertEquals("\"US\"", finalCustomerInfo["country_code"].toString())
    }

    private fun sampleRequest(
        includeBottomNote: Boolean?,
        finalCustomerInfo: FinalCustomerInfo? = null
    ): CreateOrderRequest {
        return CreateOrderRequest(
            invoice = Invoice(
                type = "01",
                operationNature = "01",
                operationDestination = "1",
            ),
            branch = Branch(code = "0000", billingPoint = "001"),
            finalCustomer = if (finalCustomerInfo != null) true else null,
            finalCustomerInfo = finalCustomerInfo,
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
