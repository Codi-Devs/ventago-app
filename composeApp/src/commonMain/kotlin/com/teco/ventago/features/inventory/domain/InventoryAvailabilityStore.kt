package com.teco.ventago.features.inventory.domain

import com.teco.ventago.core.authz.ScopeKey
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.inventory.data.InventoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class InventoryAvailabilityRow(
    val itemId: Int,
    val tracked: Boolean,
    val available: String?,
    val allowNegativeStock: Boolean = false,
    val movingAverageUnitCost: String? = null,
)

data class InventoryAvailabilitySnapshot(
    val enabled: Boolean = false,
    val locationId: Int? = null,
    val fetchedAtEpochMs: Long = 0L,
    val byItemId: Map<Int, InventoryAvailabilityRow> = emptyMap(),
) {
    fun freshnessLabel(nowEpochMs: Long = Clock.System.now().toEpochMilliseconds()): String {
        if (fetchedAtEpochMs <= 0L) return ""
        val elapsed = ((nowEpochMs - fetchedAtEpochMs).coerceAtLeast(0L) / 1000L).toInt()
        return "actualizado hace ${elapsed}s"
    }

    fun labelFor(itemId: Int): String? {
        val row = byItemId[itemId] ?: return null
        if (!row.tracked) return null
        val qty = row.available ?: "—"
        val freshness = freshnessLabel()
        return if (freshness.isBlank()) "Stock $qty" else "Stock $qty · $freshness"
    }

    fun catalogStockLabel(itemId: Int): String? {
        val row = byItemId[itemId] ?: return null
        if (!row.tracked) return null
        return "Stock ${row.available ?: "—"}"
    }

    fun shouldBlockUnstocked(
        itemId: Int,
        canView: Boolean,
        isPersonalized: Boolean,
    ): Boolean {
        if (isPersonalized || itemId <= 0 || !canView || !enabled) return false
        val row = byItemId[itemId] ?: return false
        if (!row.tracked || row.allowNegativeStock) return false
        val available = row.available?.trim().orEmpty()
        if (available.isEmpty()) return false
        val qty = available.toDoubleOrNull() ?: return false
        return qty <= 0.0
    }
}

class InventoryAvailabilityStore(
    private val provider: InventoryProvider,
    private val authService: IAuthService,
) {
    private val _snapshot = MutableStateFlow(InventoryAvailabilitySnapshot())
    val snapshot: StateFlow<InventoryAvailabilitySnapshot> = _snapshot.asStateFlow()

    fun canView(): Boolean {
        val user = authService.getUserSync() ?: return false
        if (!user.isSubUser) return true
        return ScopeKey.INVENTORY_VIEW in user.scopes
    }

    fun canReceive(): Boolean {
        val user = authService.getUserSync() ?: return false
        if (!user.isSubUser) return true
        return ScopeKey.INVENTORY_RECEIVE in user.scopes
    }

    fun canTransfer(): Boolean {
        val user = authService.getUserSync() ?: return false
        if (!user.isSubUser) return true
        return ScopeKey.INVENTORY_TRANSFER in user.scopes
    }

    fun canCount(): Boolean {
        val user = authService.getUserSync() ?: return false
        if (!user.isSubUser) return true
        return ScopeKey.INVENTORY_COUNT in user.scopes
    }

    fun canAdjust(): Boolean {
        val user = authService.getUserSync() ?: return false
        if (!user.isSubUser) return true
        return ScopeKey.INVENTORY_ADJUST in user.scopes
    }

    fun canConfigure(): Boolean {
        val user = authService.getUserSync() ?: return false
        if (!user.isSubUser) return true
        return ScopeKey.INVENTORY_CONFIGURE in user.scopes
    }

    fun shouldBlockUnstocked(itemId: Int, isPersonalized: Boolean): Boolean {
        return snapshot.value.shouldBlockUnstocked(
            itemId = itemId,
            canView = canView(),
            isPersonalized = isPersonalized,
        )
    }

    suspend fun isModuleEnabled(businessId: Int): Boolean {
        if (businessId <= 0) return false
        val response = runCatching { provider.getAccess(businessId) }.getOrNull() ?: return false
        val data = response.data as? JsonObject ?: return false
        return response.successful && (data["enabled"]?.jsonPrimitive?.booleanOrNull == true)
    }

    suspend fun refresh(
        businessId: Int,
        branchCode: String,
        billingPoint: String,
        itemIds: List<Int>,
    ) {
        if (businessId <= 0 || branchCode.isBlank() || billingPoint.isBlank() || !canView()) {
            _snapshot.value = InventoryAvailabilitySnapshot()
            return
        }
        val enabled = isModuleEnabled(businessId)
        if (!enabled) {
            _snapshot.value = InventoryAvailabilitySnapshot()
            return
        }
        val locationResponse = runCatching {
            provider.saleLocation(businessId, branchCode, billingPoint)
        }.getOrNull()
        val locationData = locationResponse?.data as? JsonObject
        val locationId = locationData?.get("location_id")?.jsonPrimitive?.intOrNull
        if (locationResponse?.successful != true || locationId == null || locationId <= 0) {
            _snapshot.value = InventoryAvailabilitySnapshot(enabled = true)
            return
        }
        loadBatch(businessId, locationId, itemIds)
    }

    suspend fun refreshForCatalog(businessId: Int, itemIds: List<Int>) {
        if (businessId <= 0 || !canView()) {
            _snapshot.value = InventoryAvailabilitySnapshot()
            return
        }
        val enabled = isModuleEnabled(businessId)
        if (!enabled) {
            _snapshot.value = InventoryAvailabilitySnapshot()
            return
        }
        val locationsResponse = runCatching { provider.listLocations(businessId) }.getOrNull()
        val locationId = firstStockableLocationId(locationsResponse?.data)
        if (locationId == null || locationId <= 0) {
            _snapshot.value = InventoryAvailabilitySnapshot(enabled = true)
            return
        }
        loadBatch(businessId, locationId, itemIds)
    }

    private suspend fun loadBatch(businessId: Int, locationId: Int, itemIds: List<Int>) {
        val uniqueIds = itemIds.filter { it > 0 }.distinct().take(100)
        if (uniqueIds.isEmpty()) {
            _snapshot.value = InventoryAvailabilitySnapshot(
                enabled = true,
                locationId = locationId,
                fetchedAtEpochMs = Clock.System.now().toEpochMilliseconds(),
            )
            return
        }
        val batchResponse = runCatching {
            provider.availabilityBatch(businessId, uniqueIds.map { it to locationId })
        }.getOrNull()
        val data = batchResponse?.data as? JsonObject
        val rows = data?.get("items")?.jsonArray.orEmpty().mapNotNull { element ->
            val obj = element.jsonObject
            val itemId = obj["item_id"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
            InventoryAvailabilityRow(
                itemId = itemId,
                tracked = obj.jsonTruthy("is_inventory_tracked"),
                available = obj["available"]?.jsonPrimitive?.contentOrNull,
                allowNegativeStock = obj.jsonTruthy("allow_negative_stock"),
                movingAverageUnitCost = obj["moving_average_unit_cost"]?.jsonPrimitive?.contentOrNull,
            )
        }.associateBy { it.itemId }
        _snapshot.value = InventoryAvailabilitySnapshot(
            enabled = true,
            locationId = locationId,
            fetchedAtEpochMs = Clock.System.now().toEpochMilliseconds(),
            byItemId = rows,
        )
    }
}

private fun firstStockableLocationId(data: JsonElement?): Int? {
    val array = when (data) {
        is JsonArray -> data
        is JsonObject -> data["items"]?.jsonArray
        else -> null
    } ?: return null
    val parsed = array.mapNotNull { element ->
        val obj = runCatching { element.jsonObject }.getOrNull() ?: return@mapNotNull null
        val id = obj["id"]?.jsonPrimitive?.intOrNull
            ?: obj["location_id"]?.jsonPrimitive?.intOrNull
            ?: return@mapNotNull null
        val stockable = obj.jsonTruthy("stockable") || !obj.containsKey("stockable")
        val active = !obj.containsKey("active") || obj.jsonTruthy("active")
        Triple(id, stockable, active)
    }
    return parsed.firstOrNull { it.second && it.third }?.first ?: parsed.firstOrNull()?.first
}

private fun JsonObject.jsonTruthy(key: String): Boolean {
    val primitive = runCatching { this[key]?.jsonPrimitive }.getOrNull() ?: return false
    if (primitive.booleanOrNull == true) return true
    if (primitive.intOrNull == 1) return true
    val content = primitive.contentOrNull?.trim()?.lowercase().orEmpty()
    return content == "true" || content == "1"
}
