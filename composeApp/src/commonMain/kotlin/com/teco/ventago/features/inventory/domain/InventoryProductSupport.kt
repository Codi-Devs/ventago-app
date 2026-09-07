package com.teco.ventago.features.inventory.domain

import com.teco.ventago.features.business.domain.BusinessService
import com.teco.ventago.features.inventory.data.InventoryProvider
import com.teco.ventago.features.product.ui.item.add.viewmodel.ItemState
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class InventoryProductSection(
    val visible: Boolean = false,
    val canView: Boolean = false,
    val canConfigure: Boolean = false,
    val tracked: Boolean = false,
    val available: String = "",
    val avgCost: String = "",
    val minQty: String = "",
    val negativePolicyIndex: Int = 0,
    val profileVersion: Int = 0,
)

class InventoryProductSupport(
    private val provider: InventoryProvider,
    private val store: InventoryAvailabilityStore,
    private val businessService: BusinessService,
    private val cache: InventoryLocalCache,
) {
    suspend fun load(itemId: Int?, force: Boolean = false): InventoryProductSection {
        val businessId = businessService.business.value?.businessId ?: 0
        if (businessId <= 0) return InventoryProductSection()
        val enabled = store.isModuleEnabled(businessId, force)
        val canView = store.canView()
        val canConfigure = store.canConfigure()
        if (!enabled || (!canView && !canConfigure)) {
            return InventoryProductSection()
        }
        if (itemId == null || itemId <= 0) {
            if (!canConfigure) return InventoryProductSection()
            return InventoryProductSection(
                visible = true,
                canView = canView,
                canConfigure = true,
            )
        }
        val cachedProfile = if (force) null else cache.peekItemProfile(businessId, itemId)
        val cachedBalances = if (force) null else cache.peekBalances(businessId, itemId)
        if (cachedProfile != null) {
            return InventoryProductSection(
                visible = true,
                canView = canView,
                canConfigure = canConfigure,
                tracked = cachedProfile.tracked,
                available = if (canView && cachedProfile.tracked) cachedBalances?.available.orEmpty() else "",
                avgCost = if (canView && cachedProfile.tracked) cachedBalances?.avgCost.orEmpty() else "",
                minQty = InventoryKardexSupport.formatInventoryQuantity(cachedProfile.minQty),
                negativePolicyIndex = cachedProfile.negativePolicyIndex,
                profileVersion = cachedProfile.profileVersion,
            )
        }
        val profileData = runCatching { provider.itemProfile(businessId, itemId) }.getOrNull()?.data as? JsonObject
        val tracked = profileData.jsonTruthy("is_inventory_tracked")
        val overrideEl = profileData?.get("allow_negative_stock_override")
        val negativeIndex = when {
            overrideEl == null || overrideEl is JsonNull -> 0
            overrideEl.jsonPrimitive.booleanOrNull == true || overrideEl.jsonPrimitive.intOrNull == 1 -> 1
            else -> 2
        }
        var available = ""
        var avgCost = ""
        if (canView && tracked) {
            val balances = runCatching { provider.balancesLegacyAvailableOnly(businessId, itemId) }.getOrNull()?.data
            available = sumAvailable(balances)
            avgCost = firstAvgCost(balances)
            cache.persistBalances(businessId, itemId, CachedBalances(available = available, avgCost = avgCost))
        }
        val profile = CachedItemProfile(
            tracked = tracked,
            minQty = profileData?.get("min_qty")?.jsonPrimitive?.contentOrNull.orEmpty(),
            negativePolicyIndex = negativeIndex,
            profileVersion = profileData?.get("version")?.jsonPrimitive?.intOrNull ?: 0,
        )
        cache.persistItemProfile(businessId, itemId, profile)
        return InventoryProductSection(
            visible = true,
            canView = canView,
            canConfigure = canConfigure,
            tracked = tracked,
            available = available,
            avgCost = avgCost,
            minQty = InventoryKardexSupport.formatInventoryQuantity(profile.minQty),
            negativePolicyIndex = negativeIndex,
            profileVersion = profile.profileVersion,
        )
    }

    suspend fun save(itemId: Int, state: ItemState): String? {
        if (!state.inventorySectionVisible || !state.inventoryCanConfigure || itemId <= 0) return null
        val businessId = businessService.business.value?.businessId ?: 0
        if (businessId <= 0) return "No se pudo guardar la configuración de inventario."
        val override = when (state.inventoryNegativePolicyIndex) {
            1 -> true
            2 -> false
            else -> null
        }
        val response = runCatching {
            provider.updateItemProfile(
                businessId = businessId,
                itemId = itemId,
                tracked = state.inventoryTracked,
                allowNegativeOverride = override,
                expectedVersion = state.inventoryProfileVersion,
                minQty = state.inventoryMinQty.trim().ifBlank { null },
                reorderQty = null,
            )
        }.getOrNull()
        return if (response?.successful == true) {
            cache.invalidateBusiness(businessId)
            null
        } else {
            "No se pudo guardar la configuración de inventario."
        }
    }

    fun apply(section: InventoryProductSection, state: ItemState): ItemState = state.copy(
        inventorySectionVisible = section.visible,
        inventoryCanView = section.canView,
        inventoryCanConfigure = section.canConfigure,
        inventoryTracked = section.tracked,
        inventoryAvailable = InventoryKardexSupport.formatInventoryQuantity(section.available),
        inventoryAvgCost = section.avgCost,
        inventoryMinQty = InventoryKardexSupport.formatInventoryQuantity(section.minQty),
        inventoryNegativePolicyIndex = section.negativePolicyIndex,
        inventoryProfileVersion = section.profileVersion,
    )
}

private fun JsonObject?.jsonTruthy(key: String): Boolean {
    val primitive = runCatching { this?.get(key)?.jsonPrimitive }.getOrNull() ?: return false
    if (primitive.booleanOrNull == true) return true
    if (primitive.intOrNull == 1) return true
    val content = primitive.contentOrNull?.trim()?.lowercase().orEmpty()
    return content == "true" || content == "1"
}

private fun sumAvailable(data: JsonElement?): String {
    val array = when (data) {
        is JsonArray -> data
        is JsonObject -> data["items"]?.jsonArray ?: data["rows"]?.jsonArray
        else -> null
    } ?: return ""
    var total = 0.0
    var any = false
    array.forEach { element ->
        val obj = runCatching { element.jsonObject }.getOrNull() ?: return@forEach
        val qty = obj["available"]?.jsonPrimitive?.contentOrNull
            ?: obj["quantity"]?.jsonPrimitive?.contentOrNull
        val parsed = qty?.toDoubleOrNull() ?: return@forEach
        total += parsed
        any = true
    }
    if (!any) return ""
    return InventoryKardexSupport.formatQty(total)
}

private fun firstAvgCost(data: JsonElement?): String {
    val array = when (data) {
        is JsonArray -> data
        is JsonObject -> data["items"]?.jsonArray ?: data["rows"]?.jsonArray
        else -> null
    } ?: return ""
    array.forEach { element ->
        val obj = runCatching { element.jsonObject }.getOrNull() ?: return@forEach
        val cost = obj["moving_average_unit_cost"]?.jsonPrimitive?.contentOrNull
            ?: obj["unit_cost"]?.jsonPrimitive?.contentOrNull
        if (!cost.isNullOrBlank()) return cost
    }
    return ""
}
