package com.teco.ventago.features.inventory.domain

import com.teco.ventago.core.LocalStorage
import com.teco.ventago.features.inventory.ui.InventoryDashboardSummary
import com.teco.ventago.features.inventory.ui.InventoryLocationOption
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class InventoryLocalCache(
    private val storage: LocalStorage,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun peekAccess(businessId: Int): Boolean? {
        val raw = storage.string(accessKey(businessId)) ?: return null
        return when (raw.trim().lowercase()) {
            "true", "1" -> true
            "false", "0" -> false
            else -> null
        }
    }

    fun persistAccess(businessId: Int, enabled: Boolean) {
        if (businessId <= 0) return
        storage.set(accessKey(businessId), if (enabled) "true" else "false")
    }

    fun peekSaleLocationId(businessId: Int, branchCode: String, billingPoint: String): Int? {
        val raw = storage.string(saleLocationKey(businessId, branchCode, billingPoint)) ?: return null
        return raw.toIntOrNull()?.takeIf { it > 0 }
    }

    fun persistSaleLocationId(businessId: Int, branchCode: String, billingPoint: String, locationId: Int) {
        if (businessId <= 0 || locationId <= 0) return
        storage.set(saleLocationKey(businessId, branchCode, billingPoint), locationId.toString())
    }

    fun peekAvailability(businessId: Int, locationId: Int, itemId: Int): InventoryAvailabilityRow? {
        val raw = storage.string(availabilityKey(businessId, locationId, itemId)) ?: return null
        return runCatching { json.decodeFromString<CachedAvailabilityRow>(raw).toDomain() }.getOrNull()
    }

    fun persistAvailability(businessId: Int, locationId: Int, row: InventoryAvailabilityRow, fetchedAtEpochMs: Long) {
        if (businessId <= 0 || locationId <= 0 || row.itemId <= 0) return
        storage.set(
            availabilityKey(businessId, locationId, row.itemId),
            json.encodeToString(CachedAvailabilityRow.from(row, fetchedAtEpochMs)),
        )
    }

    fun peekItemProfile(businessId: Int, itemId: Int): CachedItemProfile? {
        val raw = storage.string(itemProfileKey(businessId, itemId)) ?: return null
        return runCatching { json.decodeFromString<CachedItemProfile>(raw) }.getOrNull()
    }

    fun persistItemProfile(businessId: Int, itemId: Int, profile: CachedItemProfile) {
        if (businessId <= 0 || itemId <= 0) return
        storage.set(itemProfileKey(businessId, itemId), json.encodeToString(profile))
    }

    fun peekBalances(businessId: Int, itemId: Int): CachedBalances? {
        val raw = storage.string(balancesKey(businessId, itemId)) ?: return null
        return runCatching { json.decodeFromString<CachedBalances>(raw) }.getOrNull()
    }

    fun peekKardex(businessId: Int, itemId: Int): CachedKardex? {
        val raw = storage.string(kardexKey(businessId, itemId)) ?: return null
        return runCatching { json.decodeFromString<CachedKardex>(raw) }.getOrNull()
    }

    fun persistKardex(businessId: Int, itemId: Int, kardex: CachedKardex) {
        if (businessId <= 0 || itemId <= 0) return
        storage.set(kardexKey(businessId, itemId), json.encodeToString(kardex))
    }

    fun persistBalances(businessId: Int, itemId: Int, balances: CachedBalances) {
        if (businessId <= 0 || itemId <= 0) return
        storage.set(balancesKey(businessId, itemId), json.encodeToString(balances))
    }

    fun peekDashboard(businessId: Int): InventoryDashboardSummary? {
        val raw = storage.string(dashboardKey(businessId)) ?: return null
        return runCatching { json.decodeFromString<InventoryDashboardSummary>(raw) }.getOrNull()
    }

    fun persistDashboard(businessId: Int, summary: InventoryDashboardSummary) {
        if (businessId <= 0) return
        storage.set(dashboardKey(businessId), json.encodeToString(summary))
    }

    fun peekLocations(businessId: Int): List<InventoryLocationOption>? {
        val raw = storage.string(locationsKey(businessId)) ?: return null
        return runCatching { json.decodeFromString<List<InventoryLocationOption>>(raw) }.getOrNull()
    }

    fun persistLocations(businessId: Int, locations: List<InventoryLocationOption>) {
        if (businessId <= 0) return
        storage.set(locationsKey(businessId), json.encodeToString(locations))
    }

    fun invalidateBusiness(businessId: Int) {
        if (businessId <= 0) return
        val prefix = "cache:inventory:"
        storage.allKeys()
            .filter { key ->
                key.startsWith(prefix) && inventoryCacheKeyBelongsToBusiness(key, businessId)
            }
            .forEach { storage.deleteObject(it) }
    }

    companion object {
        fun inventoryCacheKeyBelongsToBusiness(key: String, businessId: Int): Boolean {
            return Regex("^cache:inventory:[^:]+:$businessId(?::|$)").containsMatchIn(key)
        }

        fun accessKey(businessId: Int) = "cache:inventory:access:$businessId"
        fun saleLocationKey(businessId: Int, branchCode: String, billingPoint: String) =
            "cache:inventory:sale-location:$businessId:$branchCode:$billingPoint"
        fun availabilityKey(businessId: Int, locationId: Int, itemId: Int) =
            "cache:inventory:availability:$businessId:$locationId:$itemId"
        fun itemProfileKey(businessId: Int, itemId: Int) = "cache:inventory:item-profile:$businessId:$itemId"
        fun balancesKey(businessId: Int, itemId: Int) = "cache:inventory:balances:$businessId:$itemId"
        fun kardexKey(businessId: Int, itemId: Int) = "cache:inventory:kardex:$businessId:$itemId"
        fun dashboardKey(businessId: Int) = "cache:inventory:dashboard:$businessId"
        fun locationsKey(businessId: Int) = "cache:inventory:locations:$businessId"
    }
}

@Serializable
data class CachedAvailabilityRow(
    val itemId: Int,
    val tracked: Boolean,
    val available: String? = null,
    val allowNegativeStock: Boolean = false,
    val movingAverageUnitCost: String? = null,
    val fetchedAtEpochMs: Long = 0L,
) {
    fun toDomain() = InventoryAvailabilityRow(
        itemId = itemId,
        tracked = tracked,
        available = available?.let { InventoryKardexSupport.formatInventoryQuantity(it) },
        allowNegativeStock = allowNegativeStock,
        movingAverageUnitCost = movingAverageUnitCost,
        fetchedAtEpochMs = fetchedAtEpochMs,
    )

    companion object {
        fun from(row: InventoryAvailabilityRow, fetchedAtEpochMs: Long) = CachedAvailabilityRow(
            itemId = row.itemId,
            tracked = row.tracked,
            available = row.available,
            allowNegativeStock = row.allowNegativeStock,
            movingAverageUnitCost = row.movingAverageUnitCost,
            fetchedAtEpochMs = fetchedAtEpochMs,
        )
    }
}

@Serializable
data class CachedItemProfile(
    val tracked: Boolean = false,
    val minQty: String = "",
    val negativePolicyIndex: Int = 0,
    val profileVersion: Int = 0,
)

@Serializable
data class CachedBalances(
    val available: String = "",
    val avgCost: String = "",
    val rows: List<CachedBalanceRow> = emptyList(),
)

@Serializable
data class CachedBalanceRow(
    val locationId: Int,
    val locationName: String,
    val stockState: String,
    val quantity: String,
)

@Serializable
data class CachedKardex(
    val rows: List<CachedKardexRow> = emptyList(),
    val nextBeforeMovementId: Int? = null,
    val fetchedAtEpochMs: Long = 0L,
)

@Serializable
data class CachedKardexRow(
    val movementId: Int,
    val movementType: String,
    val displayType: String? = null,
    val locationId: Int,
    val locationName: String? = null,
    val locationCode: String? = null,
    val direction: String,
    val quantity: String,
    val occurredAt: String,
    val postedAt: String,
    val stockState: String,
    val lineNo: Int = 0,
    val sourceType: String? = null,
    val sourceId: String? = null,
    val correlationId: String? = null,
    val reasonText: String? = null,
) {
    fun toDomain() = KardexMovementRow(
        movementId = movementId,
        movementType = movementType,
        displayType = displayType,
        locationId = locationId,
        locationName = locationName,
        locationCode = locationCode,
        direction = direction,
        quantity = quantity,
        occurredAt = occurredAt,
        postedAt = postedAt,
        stockState = stockState,
        lineNo = lineNo,
        sourceType = sourceType,
        sourceId = sourceId,
        correlationId = correlationId,
        reasonText = reasonText,
    )

    companion object {
        fun from(row: KardexMovementRow) = CachedKardexRow(
            movementId = row.movementId,
            movementType = row.movementType,
            displayType = row.displayType,
            locationId = row.locationId,
            locationName = row.locationName,
            locationCode = row.locationCode,
            direction = row.direction,
            quantity = row.quantity,
            occurredAt = row.occurredAt,
            postedAt = row.postedAt,
            stockState = row.stockState,
            lineNo = row.lineNo,
            sourceType = row.sourceType,
            sourceId = row.sourceId,
            correlationId = row.correlationId,
            reasonText = row.reasonText,
        )
    }
}
