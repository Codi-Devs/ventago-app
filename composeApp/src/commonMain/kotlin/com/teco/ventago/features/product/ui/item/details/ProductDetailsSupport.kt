package com.teco.ventago.features.product.ui.item.details

import androidx.compose.ui.graphics.Color
import com.teco.ventago.design_system.theme.Online
import com.teco.ventago.design_system.theme.RedLight
import com.teco.ventago.design_system.theme.WarningAmber
import com.teco.ventago.features.inventory.domain.ProductInventoryDetails
import com.teco.ventago.features.product.domain.model.AdditionalInfoKey
import com.teco.ventago.features.product.domain.model.Item
import com.teco.ventago.features.product.ui.item.add.viewmodel.AdditionalEntryUI
import kotlinx.serialization.json.JsonPrimitive
import kotlin.math.roundToInt

data class ProductMarginMeta(
    val amount: Double,
    val percent: Double,
    val color: Color,
    val costSourceLabel: String,
)

fun Item.parseAdditionalEntries(): List<AdditionalEntryUI> {
    return additionalInfo?.entries?.mapNotNull { (key, value) ->
        val infoKey = AdditionalInfoKey.fromKey(key) ?: return@mapNotNull null
        val raw = when (value) {
            is JsonPrimitive -> value.content
            else -> value.toString()
        }.trim()
        if (raw.isEmpty()) return@mapNotNull null
        AdditionalEntryUI(
            keyName = infoKey.keyName,
            title = infoKey.title,
            rawValue = raw,
        )
    }.orEmpty()
}

fun resolveProductMargin(
    price: Double,
    catalogCost: Double?,
    inventory: ProductInventoryDetails,
): ProductMarginMeta {
    val inventoryAvg = inventory.avgCost.trim().toDoubleOrNull()
    val useInventory = inventory.tracked &&
        inventory.visible &&
        inventoryAvg != null &&
        inventoryAvg > 0.0
    val unitCost = when {
        useInventory -> inventoryAvg ?: 0.0
        catalogCost != null && catalogCost > 0.0 -> catalogCost
        else -> 0.0
    }
    val costSourceLabel = when {
        useInventory -> "Promedio de inventario"
        catalogCost != null && catalogCost > 0.0 -> "Costo de catálogo"
        else -> "Sin costo"
    }
    val amount = price - unitCost
    val percent = if (price > 0.0) (amount / price) * 100.0 else 0.0
    val color = when {
        price <= 0.0 -> Color.Unspecified
        percent >= 30.0 -> Online
        percent >= 15.0 -> WarningAmber
        else -> RedLight
    }
    return ProductMarginMeta(
        amount = amount,
        percent = percent,
        color = color,
        costSourceLabel = costSourceLabel,
    )
}

fun formatMarginPercent(percent: Double): String = "${percent.roundToInt()}%"

fun formatItbmsLabel(taxPercent: Int?): String {
    return when (taxPercent) {
        null -> "No configurado"
        0 -> "Exento (0%)"
        else -> "$taxPercent%"
    }
}

fun fiscalAdditionalEntries(entries: List<AdditionalEntryUI>): List<AdditionalEntryUI> {
    val priority = listOf(
        AdditionalInfoKey.PANAMA_GOODS_SERVICES_CODE.keyName,
        AdditionalInfoKey.PANAMA_GOODS_SERVICES_UNIT_CODE.keyName,
    )
    return entries.sortedBy { entry ->
        val index = priority.indexOf(entry.keyName)
        if (index >= 0) index else priority.size + 1
    }
}
