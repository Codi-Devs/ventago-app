package com.teco.ventago.features.pos

import com.teco.ventago.design_system.molecules.pos.GlobalDiscountMode
import com.teco.ventago.features.customers.domain.models.CustomerListItem
import com.teco.ventago.features.pos.domain.models.CartLine
import com.teco.ventago.features.pos.domain.models.Discount
import com.teco.ventago.features.pos.domain.models.Tax
import com.teco.ventago.features.pos.ui.viewmodel.OrderCreationCartLineCheckpoint
import com.teco.ventago.features.pos.ui.viewmodel.OrderCreationCheckpoint
import com.teco.ventago.features.pos.ui.viewmodel.OrderCreationCheckpointData
import com.teco.ventago.features.pos.ui.viewmodel.OrderCreationStep
import com.teco.ventago.features.pos.ui.viewmodel.hasMeaningfulUserData
import com.teco.ventago.features.product.domain.model.Item
import com.teco.ventago.features.product.domain.model.ProductType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OrderCreationCheckpointTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun checkpointRoundTripsThroughJson() {
        val checkpoint = OrderCreationCheckpoint(
            businessId = 42,
            savedAtEpochSeconds = 1_789_000_000,
            currentStep = OrderCreationStep.CART,
            data = OrderCreationCheckpointData(
                customer = customer(),
                cart = listOf(OrderCreationCartLineCheckpoint.from(cartLine())),
                productSnapshots = mapOf("line-1" to product()),
                taxExempt = true,
                globalDiscountMode = GlobalDiscountMode.FIXED,
                globalDiscountFixedCents = 250,
                retentionCodeIndex = 1,
            )
        )

        val encoded = json.encodeToString(checkpoint)
        val decoded = json.decodeFromString<OrderCreationCheckpoint>(encoded)

        assertEquals(checkpoint.businessId, decoded.businessId)
        assertEquals(OrderCreationStep.CART, decoded.currentStep)
        assertEquals("Acme Corp", decoded.data.customer?.name)
        assertEquals(1, decoded.data.cart.size)
        assertEquals(250, decoded.data.globalDiscountFixedCents)
        assertEquals(product().name, decoded.data.productSnapshots["line-1"]?.name)
    }

    @Test
    fun defaultCheckpointDataIsNotMeaningful() {
        assertFalse(OrderCreationCheckpointData().hasMeaningfulUserData())
    }

    @Test
    fun defaultBottomNoteConfigurationIsNotMeaningful() {
        assertFalse(OrderCreationCheckpointData(includeBottomNote = true).hasMeaningfulUserData())
        assertFalse(OrderCreationCheckpointData(includeBottomNote = false).hasMeaningfulUserData())
    }

    @Test
    fun meaningfulDataDetectionCoversCustomerCartAdjustmentsAndInvoiceInfo() {
        assertTrue(OrderCreationCheckpointData(customer = customer()).hasMeaningfulUserData())
        assertTrue(OrderCreationCheckpointData(finalCustomer = true, finalName = "Cliente final").hasMeaningfulUserData())
        assertTrue(OrderCreationCheckpointData(referencedNoteCUFE = "abc").hasMeaningfulUserData())
        assertTrue(OrderCreationCheckpointData(cart = listOf(OrderCreationCartLineCheckpoint.from(cartLine()))).hasMeaningfulUserData())
        assertTrue(OrderCreationCheckpointData(globalShippingCents = 100).hasMeaningfulUserData())
        assertTrue(OrderCreationCheckpointData(taxExempt = true).hasMeaningfulUserData())
        assertTrue(OrderCreationCheckpointData(logisticsInfo = "Entrega parcial").hasMeaningfulUserData())
        assertTrue(OrderCreationCheckpointData(retentionCodeIndex = 2).hasMeaningfulUserData())
    }

    @Test
    fun cartLineCheckpointRestoresProductSpecificFields() {
        val checkpoint = OrderCreationCartLineCheckpoint.from(cartLine())
        val restored = checkpoint.toCartLine()

        assertEquals("line-1", restored.lineId)
        assertEquals(10, restored.itemId)
        assertEquals("Personalized item", restored.name)
        assertEquals(1_500, restored.baseUnitPrice)
        assertEquals(1_250, restored.overrideUnitPrice)
        assertEquals(2.5, restored.quantity)
        assertEquals(300, (restored.discount as Discount.Amount).value)
        assertEquals(700, assertNotNull(restored.tax).rateBps)
        assertEquals(150, restored.shippingCents)
        assertEquals(80, restored.insuranceCents)
        assertEquals("L-001", restored.pharmaBatchNumber)
        assertEquals(2, restored.pharmaBatchQty)
        assertEquals(900, restored.costCents)
    }

    private fun cartLine(): CartLine = CartLine(
        lineId = "line-1",
        itemId = 10,
        name = "Personalized item",
        baseUnitPrice = 1_500,
        quantity = 2.5,
        overrideUnitPrice = 1_250,
        discount = Discount.Amount(300),
        tax = Tax(id = 7, name = "ITBMS", rateBps = 700),
        notes = "note",
        shippingCents = 150,
        insuranceCents = 80,
        pharmaBatchNumber = "L-001",
        pharmaBatchQty = 2,
        costCents = 900,
    )

    private fun customer(): CustomerListItem = CustomerListItem(
        id = 99,
        name = "Acme Corp",
        email = "billing@example.com",
        ruc = "123",
        status = 1,
        invoiceCustomer = 11,
        updatedAt = 1_789_000_000,
    )

    private fun product(): Item = Item(
        itemId = 10,
        barcode = null,
        sku = "SKU-10",
        name = "Personalized item",
        description = "Saved snapshot",
        img = "",
        price = 12.50,
        cost = 9.00,
        active = true,
        order = 1,
        taxPercent = 7,
        productType = ProductType.GOOD,
        unitMeasureCode = "und",
    )
}
