package com.teco.ventago.features.product.ui.item.details

import com.teco.ventago.design_system.theme.Online
import com.teco.ventago.design_system.theme.RedLight
import com.teco.ventago.design_system.theme.WarningAmber
import com.teco.ventago.features.inventory.domain.ProductInventoryDetails
import com.teco.ventago.features.product.domain.model.AdditionalInfoKey
import com.teco.ventago.features.product.domain.model.Item
import com.teco.ventago.features.product.ui.item.add.viewmodel.AdditionalEntryUI
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProductDetailsSupportTest {

    @Test
    fun formatsItbmsLabel() {
        assertEquals("Exento (0%)", formatItbmsLabel(0))
        assertEquals("7%", formatItbmsLabel(7))
    }

    @Test
    fun resolvesMarginWithInventoryAverage() {
        val margin = resolveProductMargin(
            price = 10.0,
            catalogCost = 4.0,
            inventory = ProductInventoryDetails(
                visible = true,
                tracked = true,
                avgCost = "6",
            ),
        )
        assertEquals(4.0, margin.amount)
        assertEquals(40.0, margin.percent)
        assertEquals("Promedio de inventario", margin.costSourceLabel)
    }

    @Test
    fun colorsMarginLikeWebThresholds() {
        val high = resolveProductMargin(100.0, 60.0, ProductInventoryDetails())
        val medium = resolveProductMargin(100.0, 80.0, ProductInventoryDetails())
        val low = resolveProductMargin(100.0, 90.0, ProductInventoryDetails())
        assertEquals(Online, high.color)
        assertEquals(WarningAmber, medium.color)
        assertEquals(RedLight, low.color)
    }

    @Test
    fun parsesAdditionalInfoEntries() {
        val item = Item(
            itemId = 1,
            barcode = null,
            sku = null,
            name = "Test",
            description = "",
            img = "",
            price = 1.0,
            cost = null,
            active = true,
            order = 0,
            taxPercent = 7,
            productType = com.teco.ventago.features.product.domain.model.ProductType.GOOD,
            additionalInfo = buildJsonObject {
                put(
                    AdditionalInfoKey.PANAMA_GOODS_SERVICES_CODE.keyName,
                    JsonPrimitive("2519"),
                )
            },
        )
        val entries = item.parseAdditionalEntries()
        assertEquals(1, entries.size)
        assertEquals("2519", entries.first().rawValue)
    }

    @Test
    fun prioritizesPanamaFiscalEntries() {
        val entries = fiscalAdditionalEntries(
            listOf(
                AdditionalEntryUI("fabrication_date", "Fecha", "2026-01-01"),
                AdditionalEntryUI(
                    AdditionalInfoKey.PANAMA_GOODS_SERVICES_UNIT_CODE.keyName,
                    "Unidad",
                    "und",
                ),
                AdditionalEntryUI(
                    AdditionalInfoKey.PANAMA_GOODS_SERVICES_CODE.keyName,
                    "Código",
                    "2519",
                ),
            ),
        )
        assertTrue(entries.first().keyName == AdditionalInfoKey.PANAMA_GOODS_SERVICES_CODE.keyName)
    }
}
