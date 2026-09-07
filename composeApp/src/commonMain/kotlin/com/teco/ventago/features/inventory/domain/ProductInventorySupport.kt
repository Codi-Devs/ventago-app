package com.teco.ventago.features.inventory.domain

import com.teco.ventago.features.inventory.data.InventoryProvider
import com.teco.ventago.features.inventory.ui.InventoryLocationOption
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

class ProductInventorySupport(
    private val provider: InventoryProvider,
    private val store: InventoryAvailabilityStore,
    private val cache: InventoryLocalCache,
) {
    suspend fun loadDetails(businessId: Int, itemId: Int, force: Boolean = false): ProductInventoryDetails {
        if (businessId <= 0 || itemId <= 0) return ProductInventoryDetails()
        val enabled = store.isModuleEnabled(businessId, force)
        val canView = store.canView()
        if (!enabled || !canView) return ProductInventoryDetails()

        val cachedProfile = if (force) null else cache.peekItemProfile(businessId, itemId)
        val profileData = if (cachedProfile != null) {
            null
        } else {
            runCatching { provider.itemProfile(businessId, itemId) }.getOrNull()?.data as? JsonObject
        }
        val tracked = cachedProfile?.tracked == true ||
            profileData.jsonTruthy("is_inventory_tracked")
        if (!tracked) {
            return ProductInventoryDetails(visible = true, tracked = false)
        }

        val minQty = InventoryKardexSupport.formatInventoryQuantity(
            cachedProfile?.minQty
                ?: profileData?.get("min_qty")?.jsonPrimitive?.contentOrNull.orEmpty(),
        )

        val cachedLocations = if (force) null else cache.peekLocations(businessId)
        val locations = cachedLocations ?: run {
            val response = runCatching { provider.listLocations(businessId) }.getOrNull()
            parseLocationOptions(response?.data).also { parsed ->
                if (parsed.isNotEmpty()) cache.persistLocations(businessId, parsed)
            }
        }

        val cachedBalances = if (force) null else cache.peekBalances(businessId, itemId)
        val cachedKardex = if (force) null else cache.peekKardex(businessId, itemId)

        val balanceRows = when {
            cachedBalances?.rows?.isNotEmpty() == true -> cachedBalances.rows.map {
                InventoryBalanceRow(
                    locationId = it.locationId,
                    locationName = it.locationName,
                    stockState = it.stockState,
                    quantity = InventoryKardexSupport.formatInventoryQuantity(it.quantity),
                )
            }
            else -> {
                val response = runCatching { provider.balances(businessId, itemId) }.getOrNull()
                InventoryKardexSupport.parseBalances(response?.data, locations).also { rows ->
                    if (rows.isNotEmpty()) {
                        val available = InventoryKardexSupport.formatQty(
                            InventoryKardexSupport.sumBalanceState(rows, "available"),
                        )
                        cache.persistBalances(
                            businessId,
                            itemId,
                            CachedBalances(
                                available = available,
                                rows = rows.map {
                                    CachedBalanceRow(
                                        it.locationId,
                                        it.locationName,
                                        it.stockState,
                                        it.quantity,
                                    )
                                },
                            ),
                        )
                    }
                }
            }
        }

        val kardexRows = when {
            cachedKardex != null && cachedKardex.rows.isNotEmpty() -> {
                cachedKardex.rows.map { it.toDomain() }
            }
            else -> {
                val response = runCatching {
                    provider.kardex(businessId, itemId)
                }.getOrNull()
                val parsed = InventoryKardexSupport.parseKardexResponse(response?.data)
                if (parsed.rows.isNotEmpty()) {
                    cache.persistKardex(
                        businessId,
                        itemId,
                        CachedKardex(
                            rows = parsed.rows.map { CachedKardexRow.from(it) },
                            nextBeforeMovementId = parsed.nextBeforeMovementId,
                            fetchedAtEpochMs = Clock.System.now().toEpochMilliseconds(),
                        ),
                    )
                }
                parsed.rows
            }
        }

        val availableQty = InventoryKardexSupport.sumBalanceState(balanceRows, "available")
        val reservedQty = InventoryKardexSupport.sumBalanceState(balanceRows, "reserved")
        val (alarmLabel, alarmIsAlert) = InventoryKardexSupport.alarmState(availableQty, minQty)
        val displayKardex = InventoryKardexSupport.collapseKardexRows(kardexRows)

        val avgCost = cachedBalances?.avgCost?.takeIf { it.isNotBlank() }
            ?: run {
                val response = runCatching { provider.valuation(businessId, itemId) }.getOrNull()?.data as? JsonObject
                response?.get("moving_average_unit_cost")?.jsonPrimitive?.contentOrNull
                    ?: response?.get("average_unit_cost")?.jsonPrimitive?.contentOrNull
                    ?: ""
            }

        if (avgCost.isNotBlank() && cachedBalances != null && cachedBalances.avgCost.isBlank()) {
            cache.persistBalances(
                businessId,
                itemId,
                cachedBalances.copy(avgCost = avgCost),
            )
        }

        return ProductInventoryDetails(
            visible = true,
            tracked = true,
            available = InventoryKardexSupport.formatQty(availableQty),
            reserved = InventoryKardexSupport.formatQty(reservedQty),
            minQty = minQty,
            avgCost = avgCost,
            alarmLabel = alarmLabel,
            alarmIsAlert = alarmIsAlert,
            balances = balanceRows,
            chartPoints = InventoryKardexSupport.buildAvailableSeries(availableQty, displayKardex),
        )
    }

    suspend fun loadKardex(
        businessId: Int,
        itemId: Int,
        existingRows: List<KardexMovementRow> = emptyList(),
        nextCursor: Int? = null,
        force: Boolean = false,
    ): KardexPageResult {
        if (businessId <= 0 || itemId <= 0) return KardexPageResult()
        if (!force && existingRows.isEmpty() && nextCursor == null) {
            cache.peekKardex(businessId, itemId)?.let { cached ->
                if (cached.rows.isNotEmpty()) {
                    val rows = cached.rows.map { it.toDomain() }
                    val displayRows = InventoryKardexSupport.collapseKardexRows(rows)
                    return KardexPageResult(
                        rows = rows,
                        displayRows = displayRows,
                        nextBeforeMovementId = cached.nextBeforeMovementId,
                        hasMoreOnServer = cached.nextBeforeMovementId != null &&
                            rows.size >= InventoryKardexSupport.KARDEX_FETCH_LIMIT,
                    )
                }
            }
        }
        val response = runCatching {
            provider.kardex(
                businessId = businessId,
                itemId = itemId,
                beforeMovementId = nextCursor,
            )
        }.getOrNull()
        val parsed = InventoryKardexSupport.parseKardexResponse(response?.data)
        val merged = if (nextCursor != null) existingRows + parsed.rows else parsed.rows
        if (nextCursor == null && merged.isNotEmpty()) {
            cache.persistKardex(
                businessId,
                itemId,
                CachedKardex(
                    rows = merged.map { CachedKardexRow.from(it) },
                    nextBeforeMovementId = parsed.nextBeforeMovementId,
                    fetchedAtEpochMs = Clock.System.now().toEpochMilliseconds(),
                ),
            )
        }
        val displayRows = InventoryKardexSupport.collapseKardexRows(merged)
        return KardexPageResult(
            rows = merged,
            displayRows = displayRows,
            nextBeforeMovementId = parsed.nextBeforeMovementId,
            hasMoreOnServer = parsed.hasMoreOnServer,
        )
    }

    private fun parseLocationOptions(data: kotlinx.serialization.json.JsonElement?): List<InventoryLocationOption> {
        val array = when (data) {
            is kotlinx.serialization.json.JsonArray -> data
            is JsonObject -> data["items"]?.let { it as? kotlinx.serialization.json.JsonArray }
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
            InventoryLocationOption(id = id, name = name)
        }
    }

    private fun JsonObject?.jsonTruthy(key: String): Boolean {
        val primitive = runCatching { this?.get(key)?.jsonPrimitive }.getOrNull() ?: return false
        if (primitive.booleanOrNull == true) return true
        if (primitive.intOrNull == 1) return true
        val content = primitive.contentOrNull?.trim()?.lowercase().orEmpty()
        return content == "true" || content == "1"
    }
}
