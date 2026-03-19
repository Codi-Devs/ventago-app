package com.teco.ventago.features.pos.domain.models

import kotlin.test.Test
import kotlin.test.assertEquals

class CartLineTest {

    @Test
    fun lineMath_supportsFractionalQuantitiesAndRounding() {
        val line = CartLine(
            lineId = "line-1",
            itemId = 1,
            name = "Test Item",
            baseUnitPrice = 1000L,
            quantity = 1.125,
            discount = Discount.Percent(1000), // 10%
            tax = Tax(id = 700, name = "ITBMS", rateBps = 700)
        )

        assertEquals(1125L, line.lineSubtotalBeforeDiscount())
        assertEquals(113L, line.discountAmount())
        assertEquals(1013L, line.lineSubtotal())
        assertEquals(71L, line.taxTotal(taxExempt = false))
        assertEquals(1084L, line.total(orderTaxExempt = false))
    }

    @Test
    fun lineMath_handlesSmallFractionalQuantity() {
        val line = CartLine(
            lineId = "line-2",
            itemId = 2,
            name = "Quarter Item",
            baseUnitPrice = 1000L,
            quantity = 0.25,
            discount = null,
            tax = null
        )

        assertEquals(250L, line.lineSubtotalBeforeDiscount())
        assertEquals(250L, line.lineSubtotal())
        assertEquals(250L, line.total(orderTaxExempt = false))
    }
}
