package com.teco.ventago.features.quotes.domain

import com.teco.ventago.features.customers.domain.models.CustomerListItem
import com.teco.ventago.features.orders.domain.models.requests.Branch
import com.teco.ventago.features.orders.domain.models.requests.CreateOrderRequest
import com.teco.ventago.features.orders.domain.models.requests.CreateOrderTotals
import com.teco.ventago.features.orders.domain.models.requests.Invoice
import com.teco.ventago.features.orders.domain.models.requests.OrderItem
import com.teco.ventago.features.pos.domain.models.CartLine
import com.teco.ventago.features.pos.ui.viewmodel.PosState
import com.teco.ventago.features.product.domain.model.Item
import com.teco.ventago.features.product.domain.model.ProductType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class QuoteRequestBuilderTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun buildQuoteRequest_serializesDecimalQuantityAndLineCount() {
        val state = PosState(
            items = listOf(
                sampleItem(id = 1, name = "Product A", price = 10.0),
                sampleItem(id = 2, name = "Product B", price = 20.0)
            ),
            cart = listOf(
                CartLine(
                    lineId = "line-1",
                    itemId = 1,
                    name = "Product A",
                    baseUnitPrice = 1000L,
                    quantity = 1.25
                ),
                CartLine(
                    lineId = "line-2",
                    itemId = 2,
                    name = "Product B",
                    baseUnitPrice = 2000L,
                    quantity = 0.375
                )
            ),
            finalCustomer = false,
            customer = CustomerListItem(
                id = 10L,
                name = "Client",
                email = "client@demo.com",
                ruc = "123456",
                status = 1,
                invoiceCustomer = 1,
                updatedAt = 1L
            )
        )

        val request = QuoteRequestBuilder.build(state)

        assertEquals("1.2500", request.items[0].quantity)
        assertEquals("0.3750", request.items[1].quantity)
        assertEquals(2, request.totals.quantityItems)
    }

    @Test
    fun createOrderRequest_serializesItemQuantityAsStringDecimal() {
        val request = CreateOrderRequest(
            invoice = Invoice(
                type = "01",
                operationNature = "01",
                operationDestination = "1"
            ),
            branch = Branch(code = "0000", billingPoint = "001"),
            orderItems = listOf(
                OrderItem(
                    itemId = 1,
                    code = "A1",
                    name = "Product",
                    unitMeasure = "und",
                    quantity = "1.2500",
                    baseUnitPrice = "10.00"
                )
            ),
            totals = CreateOrderTotals(
                quantityItems = 1,
                subtotal = "10.00",
                totalBeforeDiscounts = "10.00",
                totalAfterDiscounts = "10.00",
                totalBeforeTaxes = "10.00",
                totalAfterTaxes = "10.00",
                totalTaxes = "0.00",
                invoiceTotal = "10.00"
            )
        )

        val payload = json.encodeToJsonElement(CreateOrderRequest.serializer(), request).jsonObject
        val firstItem = payload["items"]!!.jsonArray[0].jsonObject
        val totals = payload["totals"]!!.jsonObject

        assertEquals("\"1.2500\"", firstItem["quantity"].toString())
        assertEquals("1", totals["quantity_items"].toString())
    }

    private fun sampleItem(id: Int, name: String, price: Double): Item {
        return Item(
            itemId = id,
            barcode = "B-$id",
            sku = "SKU-$id",
            name = name,
            description = name,
            img = "",
            price = price,
            cost = null,
            active = true,
            order = 0,
            taxPercent = 0,
            productType = ProductType.GOOD,
            unitMeasureCode = "und",
            iscRate = null,
            otiTaxes = null,
            isPharma = false,
            additionalInfo = null
        )
    }
}
