package com.teco.ventago.features.inventory.domain

import com.teco.ventago.core.authz.ScopeKey
import com.teco.ventago.core.changes.IChangesManager
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.inventory.data.InventoryProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
    val fetchedAtEpochMs: Long = 0L,
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
        val qty = row.available?.let { InventoryKardexSupport.formatInventoryQuantity(it) }?.ifBlank { null } ?: "—"
        val freshness = freshnessLabel()
        return if (freshness.isBlank()) "Stock $qty" else "Stock $qty · $freshness"
    }

    fun catalogStockLabel(itemId: Int): String? {
        val row = byItemId[itemId] ?: return null
        if (!row.tracked) return null
        val qty = row.available?.let { InventoryKardexSupport.formatInventoryQuantity(it) }?.ifBlank { null } ?: "—"
        return "Stock $qty"
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
    private val cache: InventoryLocalCache,
    private val changesManager: IChangesManager,
) {
    private val _snapshot = MutableStateFlow(InventoryAvailabilitySnapshot())
    val snapshot: StateFlow<InventoryAvailabilitySnapshot> = _snapshot.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    private var lastRefresh: (suspend () -> Unit)? = null
    private var lastBusinessId: Int = 0

    init {
        changesManager.inventoryListener().onEach { value ->
            if (value == 1) return@onEach
            val businessId = lastBusinessId
            if (businessId > 0) {
                cache.invalidateBusiness(businessId)
            }
            lastRefresh?.invoke()
        }.launchIn(scope)
    }

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

    fun invalidateBusiness(businessId: Int) {
        cache.invalidateBusiness(businessId)
    }

    suspend fun isModuleEnabled(businessId: Int, force: Boolean = false): Boolean {
        if (businessId <= 0) return false
        if (!force) {
            cache.peekAccess(businessId)?.let { return it }
        }
        val response = runCatching { provider.getAccess(businessId) }.getOrNull()
        val data = response?.data as? JsonObject
        val enabled = response?.successful == true && (data?.get("enabled")?.jsonPrimitive?.booleanOrNull == true)
        if (response != null) {
            cache.persistAccess(businessId, enabled)
            return enabled
        }
        return cache.peekAccess(businessId) ?: false
    }

    suspend fun refresh(
        businessId: Int,
        branchCode: String,
        billingPoint: String,
        itemIds: List<Int>,
        force: Boolean = false,
    ) {
        lastBusinessId = businessId
        lastRefresh = { refresh(businessId, branchCode, billingPoint, itemIds, force = true) }
        if (businessId <= 0 || branchCode.isBlank() || billingPoint.isBlank() || !canView()) {
            _snapshot.value = InventoryAvailabilitySnapshot()
            return
        }
        val enabled = isModuleEnabled(businessId, force)
        if (!enabled) {
            _snapshot.value = InventoryAvailabilitySnapshot()
            return
        }
        val cachedLocationId = if (force) null else cache.peekSaleLocationId(businessId, branchCode, billingPoint)
        val locationId = cachedLocationId ?: run {
            val locationResponse = runCatching {
                provider.saleLocation(businessId, branchCode, billingPoint)
            }.getOrNull()
            val locationData = locationResponse?.data as? JsonObject
            val fetchedId = locationData?.get("location_id")?.jsonPrimitive?.intOrNull
            if (locationResponse?.successful == true && fetchedId != null && fetchedId > 0) {
                cache.persistSaleLocationId(businessId, branchCode, billingPoint, fetchedId)
                fetchedId
            } else {
                null
            }
        }
        if (locationId == null || locationId <= 0) {
            _snapshot.value = InventoryAvailabilitySnapshot(enabled = true)
            return
        }
        loadBatch(businessId, locationId, itemIds, force)
    }

    suspend fun refreshForCatalog(businessId: Int, itemIds: List<Int>, force: Boolean = false) {
        lastBusinessId = businessId
        lastRefresh = { refreshForCatalog(businessId, itemIds, force = true) }
        if (businessId <= 0 || !canView()) {
            _snapshot.value = InventoryAvailabilitySnapshot()
            return
        }
        val enabled = isModuleEnabled(businessId, force)
        if (!enabled) {
            _snapshot.value = InventoryAvailabilitySnapshot()
            return
        }
        val cachedLocations = if (force) null else cache.peekLocations(businessId)
        val locationId = if (cachedLocations != null) {
            firstStockableLocationId(cachedLocations)
        } else {
            val locationsResponse = runCatching { provider.listLocations(businessId) }.getOrNull()
            val locations = parseLocationOptions(locationsResponse?.data)
            if (locations.isNotEmpty()) {
                cache.persistLocations(businessId, locations)
            }
            firstStockableLocationId(locations)
        }
        if (locationId == null || locationId <= 0) {
            _snapshot.value = InventoryAvailabilitySnapshot(enabled = true)
            return
        }
        loadBatch(businessId, locationId, itemIds, force)
    }

    private suspend fun loadBatch(businessId: Int, locationId: Int, itemIds: List<Int>, force: Boolean) {
        val uniqueIds = itemIds.filter { it > 0 }.distinct().take(100)
        if (uniqueIds.isEmpty()) {
            _snapshot.value = InventoryAvailabilitySnapshot(
                enabled = true,
                locationId = locationId,
                fetchedAtEpochMs = Clock.System.now().toEpochMilliseconds(),
            )
            return
        }
        val peeked = if (force) {
            emptyMap()
        } else {
            uniqueIds.mapNotNull { itemId ->
                cache.peekAvailability(businessId, locationId, itemId)
            }.associateBy { it.itemId }
        }
        if (peeked.isNotEmpty()) {
            _snapshot.value = InventoryAvailabilitySnapshot(
                enabled = true,
                locationId = locationId,
                fetchedAtEpochMs = peeked.values.maxOf { it.fetchedAtEpochMs }.takeIf { it > 0 }
                    ?: Clock.System.now().toEpochMilliseconds(),
                byItemId = peeked,
            )
        }
        val missing = uniqueIds.filter { it !in peeked }
        if (missing.isEmpty()) return
        val now = Clock.System.now().toEpochMilliseconds()
        val batchResponse = runCatching {
            provider.availabilityBatch(businessId, missing.map { it to locationId })
        }.getOrNull()
        val data = batchResponse?.data as? JsonObject
        val fetched = data?.get("items")?.jsonArray.orEmpty().mapNotNull { element ->
            val obj = element.jsonObject
            val itemId = obj["item_id"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
            InventoryAvailabilityRow(
                itemId = itemId,
                tracked = obj.jsonTruthy("is_inventory_tracked"),
                available = obj["available"]?.jsonPrimitive?.contentOrNull
                    ?.let { InventoryKardexSupport.formatInventoryQuantity(it) },
                allowNegativeStock = obj.jsonTruthy("allow_negative_stock"),
                movingAverageUnitCost = obj["moving_average_unit_cost"]?.jsonPrimitive?.contentOrNull,
                fetchedAtEpochMs = now,
            )
        }.associateBy { it.itemId }
        fetched.values.forEach { row ->
            cache.persistAvailability(businessId, locationId, row, now)
        }
        val merged = peeked + fetched
        _snapshot.value = InventoryAvailabilitySnapshot(
            enabled = true,
            locationId = locationId,
            fetchedAtEpochMs = merged.values.maxOfOrNull { it.fetchedAtEpochMs }?.takeIf { it > 0 } ?: now,
            byItemId = merged,
        )
    }
}

private fun parseLocationOptions(data: JsonElement?): List<com.teco.ventago.features.inventory.ui.InventoryLocationOption> {
    val array = when (data) {
        is JsonArray -> data
        is JsonObject -> data["items"]?.jsonArray
        else -> null
    } ?: return emptyList()
    return array.mapNotNull { element ->
        val obj = runCatching { element.jsonObject }.getOrNull() ?: return@mapNotNull null
        val id = obj["id"]?.jsonPrimitive?.intOrNull
            ?: obj["location_id"]?.jsonPrimitive?.intOrNull
            ?: return@mapNotNull null
        val name = obj["name"]?.jsonPrimitive?.contentOrNull
            ?: obj["code"]?.jsonPrimitive?.contentOrNull
            ?: id.toString()
        val stockable = obj.jsonTruthy("stockable") || !obj.containsKey("stockable")
        val active = !obj.containsKey("active") || obj.jsonTruthy("active")
        if (!active) return@mapNotNull null
        com.teco.ventago.features.inventory.ui.InventoryLocationOption(
            id = id,
            name = name,
            stockable = stockable,
        )
    }
}

private fun firstStockableLocationId(
    locations: List<com.teco.ventago.features.inventory.ui.InventoryLocationOption>,
): Int? {
    return locations.firstOrNull { it.stockable }?.id ?: locations.firstOrNull()?.id
}

private fun JsonObject.jsonTruthy(key: String): Boolean {
    val primitive = runCatching { this[key]?.jsonPrimitive }.getOrNull() ?: return false
    if (primitive.booleanOrNull == true) return true
    if (primitive.intOrNull == 1) return true
    val content = primitive.contentOrNull?.trim()?.lowercase().orEmpty()
    return content == "true" || content == "1"
}
