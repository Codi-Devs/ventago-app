package com.teco.ventago.features.orders

import com.teco.ventago.features.orders.domain.models.requests.Branch
import com.teco.ventago.features.orders.domain.models.requests.CreateOrderRequest
import com.teco.ventago.features.orders.domain.models.requests.CreateOrderTotals
import com.teco.ventago.features.orders.domain.models.requests.FinalCustomerInfo
import com.teco.ventago.features.orders.domain.models.requests.Invoice
import com.teco.ventago.features.orders.domain.models.requests.PaymentLinksBlock
import com.teco.ventago.features.orders.domain.models.requests.ReferenceNumber
import com.teco.ventago.features.orders.domain.models.requests.References
import com.teco.ventago.json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonArray
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

    @Test
    fun createOrderSerializesYappyOnsitePaymentFlow() {
        val payload = json.parseToJsonElement(
            json.encodeToString(
                sampleRequest(includeBottomNote = null).copy(
                    paymentFlowType = "in_place",
                    links = PaymentLinksBlock(
                        create = true,
                        expireInMinutes = 5,
                        note = "Factura POS",
                        method = "YAPPY_ONSITE",
                    )
                )
            )
        ).jsonObject
        val links = payload["links"]!!.jsonObject

        assertEquals("\"in_place\"", payload["payment_flow_type"].toString())
        assertEquals("true", links["create"].toString())
        assertEquals("\"YAPPY_ONSITE\"", links["method"].toString())
        assertEquals("\"Factura POS\"", links["note"].toString())
    }

    @Test
    fun createOrderSerializesGenericCreditNotePaperReference() {
        val payload = json.parseToJsonElement(
            json.encodeToString(
                sampleRequest(includeBottomNote = null).copy(
                    invoice = Invoice(
                        type = "06",
                        operationNature = "01",
                        operationDestination = "1",
                    ),
                    references = listOf(
                        References(
                            legalName = "",
                            issueDatetime = "2026-07-01T00:00:00",
                            referenceNumber = ReferenceNumber(
                                type = "paper",
                                number = "1234567890123456789012"
                            )
                        )
                    )
                )
            )
        ).jsonObject
        val reference = payload["references"]!!.jsonArray.first().jsonObject
        val referenceNumber = reference["reference_number"]!!.jsonObject

        assertEquals("\"06\"", payload["invoice"]!!.jsonObject["type"].toString())
        assertEquals("\"2026-07-01T00:00:00\"", reference["issue_datetime"].toString())
        assertEquals("\"paper\"", referenceNumber["type"].toString())
        assertEquals("\"1234567890123456789012\"", referenceNumber["number"].toString())
    }

    @Test
    fun createOrderSerializesReferencedNoteCufeReference() {
        val payload = json.parseToJsonElement(
            json.encodeToString(
                sampleRequest(includeBottomNote = null).copy(
                    invoice = Invoice(
                        type = "04",
                        operationNature = "01",
                        operationDestination = "1",
                    ),
                    references = listOf(
                        References(
                            legalName = "",
                            issueDatetime = "2026-07-01T11:30:00",
                            referenceNumber = ReferenceNumber(
                                type = "cufe",
                                number = "CUFE-123"
                            )
                        )
                    )
                )
            )
        ).jsonObject
        val referenceNumber = payload["references"]!!.jsonArray.first().jsonObject["reference_number"]!!.jsonObject

        assertEquals("\"cufe\"", referenceNumber["type"].toString())
        assertEquals("\"CUFE-123\"", referenceNumber["number"].toString())
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
