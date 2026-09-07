package com.teco.ventago.features.inventory.data

import com.teco.ventago.Configs
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.inventory.domain.InventoryKardexSupport
import com.teco.ventago.utils.ApiError
import com.teco.ventago.utils.ApiResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class InventoryProvider(
    private val client: HttpClient,
    private val authService: IAuthService,
) {
    suspend fun getAccess(businessId: Int): ApiResponse {
        val res = client.get("${Configs.ordersBasePath}/api/v1/business/inventory/access?business_id=$businessId") {
            authHeaders(businessId)
            contentType(ContentType.Application.Json)
        }
        return parse(res.status, res.body(), retry = { getAccess(businessId) })
    }

    suspend fun saleLocation(businessId: Int, branchCode: String, billingPoint: String): ApiResponse {
        val res = client.post("${Configs.serverBasePath}inventory/sale-location") {
            authHeaders(businessId)
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("business_id", businessId)
                put("branch_code", branchCode)
                put("billing_point", billingPoint)
            })
        }
        return parse(res.status, res.body(), retry = { saleLocation(businessId, branchCode, billingPoint) })
    }

    suspend fun availabilityBatch(businessId: Int, items: List<Pair<Int, Int>>): ApiResponse {
        val res = client.post("${Configs.serverBasePath}inventory/availability-batch") {
            authHeaders(businessId)
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("business_id", businessId)
                put("items", buildJsonArray {
                    items.take(100).forEach { (itemId, locationId) ->
                        add(buildJsonObject {
                            put("item_id", itemId)
                            put("location_id", locationId)
                        })
                    }
                })
            })
        }
        return parse(res.status, res.body(), retry = { availabilityBatch(businessId, items) })
    }

    suspend fun physicalReturn(
        businessId: Int,
        idempotencyKey: String,
        sourceId: String,
        lines: JsonArray,
    ): ApiResponse {
        val res = client.post("${Configs.serverBasePath}inventory/return") {
            authHeaders(businessId)
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("business_id", businessId)
                put("idempotency_key", idempotencyKey)
                put("source_type", "order")
                put("source_id", sourceId)
                put("lines", lines)
            })
        }
        return parse(res.status, res.body(), retry = {
            physicalReturn(businessId, idempotencyKey, sourceId, lines)
        })
    }

    suspend fun listLocations(businessId: Int, includeRetired: Boolean = false): ApiResponse {
        val res = client.post("${Configs.serverBasePath}inventory/locations") {
            authHeaders(businessId)
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("business_id", businessId)
                if (includeRetired) {
                    put("include_retired", 1)
                }
            })
        }
        return parse(res.status, res.body(), retry = { listLocations(businessId, includeRetired) })
    }

    suspend fun ensureDefaultLocation(businessId: Int): ApiResponse {
        val res = client.post("${Configs.serverBasePath}inventory/ensure-default-location") {
            authHeaders(businessId)
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("business_id", businessId) })
        }
        return parse(res.status, res.body(), retry = { ensureDefaultLocation(businessId) })
    }

    suspend fun adjustStock(
        businessId: Int,
        idempotencyKey: String,
        reason: String,
        operation: String,
        itemId: Int,
        locationId: Int,
        quantity: String,
        unitCost: String?,
    ): ApiResponse {
        val line = buildJsonObject {
            put("operation", operation)
            put("item_id", itemId)
            put("location_id", locationId)
            put("stock_state", "available")
            put("quantity", quantity)
            if (!unitCost.isNullOrBlank()) {
                put("unit_cost", unitCost)
            }
        }
        val res = client.post("${Configs.serverBasePath}inventory/adjust") {
            authHeaders(businessId)
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("business_id", businessId)
                put("idempotency_key", idempotencyKey)
                put("reason", reason)
                put("lines", buildJsonArray { add(line) })
            })
        }
        return parse(res.status, res.body(), retry = {
            adjustStock(businessId, idempotencyKey, reason, operation, itemId, locationId, quantity, unitCost)
        })
    }

    suspend fun createLocation(
        businessId: Int,
        code: String,
        name: String,
        locationType: String,
        parentLocationId: Int?,
        stockable: Boolean,
    ): ApiResponse {
        val res = client.post("${Configs.serverBasePath}inventory/create-location") {
            authHeaders(businessId)
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("business_id", businessId)
                put("code", code)
                put("name", name)
                put("location_type", locationType)
                if (parentLocationId != null && parentLocationId > 0) {
                    put("parent_location_id", parentLocationId)
                } else {
                    put("parent_location_id", JsonNull)
                }
                put("stockable", stockable)
                put("expected_version", 0)
            })
        }
        return parse(res.status, res.body(), retry = {
            createLocation(businessId, code, name, locationType, parentLocationId, stockable)
        })
    }

    suspend fun retireLocation(
        businessId: Int,
        locationId: Int,
        reason: String,
        expectedVersion: Int,
    ): ApiResponse {
        val res = client.post("${Configs.serverBasePath}inventory/retire-location") {
            authHeaders(businessId)
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("business_id", businessId)
                put("location_id", locationId)
                put("reason", reason)
                put("expected_version", expectedVersion)
            })
        }
        return parse(res.status, res.body(), retry = {
            retireLocation(businessId, locationId, reason, expectedVersion)
        })
    }

    suspend fun listDefaults(businessId: Int): ApiResponse {
        val res = client.post("${Configs.serverBasePath}inventory/defaults") {
            authHeaders(businessId)
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("business_id", businessId) })
        }
        return parse(res.status, res.body(), retry = { listDefaults(businessId) })
    }

    suspend fun setDefault(
        businessId: Int,
        branchCode: String,
        billingPoint: String,
        locationId: Int,
        expectedVersion: Int,
    ): ApiResponse {
        val res = client.post("${Configs.serverBasePath}inventory/set-default") {
            authHeaders(businessId)
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("business_id", businessId)
                put("branch_code", branchCode)
                put("billing_point", billingPoint)
                put("location_id", locationId)
                put("expected_version", expectedVersion)
            })
        }
        return parse(res.status, res.body(), retry = {
            setDefault(businessId, branchCode, billingPoint, locationId, expectedVersion)
        })
    }

    suspend fun removeDefault(
        businessId: Int,
        branchCode: String,
        billingPoint: String,
        expectedVersion: Int,
    ): ApiResponse {
        val res = client.post("${Configs.serverBasePath}inventory/remove-default") {
            authHeaders(businessId)
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("business_id", businessId)
                put("branch_code", branchCode)
                put("billing_point", billingPoint)
                put("expected_version", expectedVersion)
            })
        }
        return parse(res.status, res.body(), retry = {
            removeDefault(businessId, branchCode, billingPoint, expectedVersion)
        })
    }

    suspend fun transfer(
        businessId: Int,
        idempotencyKey: String,
        fromLocationId: Int,
        toLocationId: Int,
        itemId: Int,
        quantity: String,
        reason: String,
    ): ApiResponse {
        val res = client.post("${Configs.serverBasePath}inventory/transfer") {
            authHeaders(businessId)
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("business_id", businessId)
                put("idempotency_key", idempotencyKey)
                put("from_location_id", fromLocationId)
                put("to_location_id", toLocationId)
                put("reason", reason)
                put("lines", buildJsonArray {
                    add(buildJsonObject {
                        put("item_id", itemId)
                        put("quantity", quantity)
                    })
                })
            })
        }
        return parse(res.status, res.body(), retry = {
            transfer(businessId, idempotencyKey, fromLocationId, toLocationId, itemId, quantity, reason)
        })
    }

    suspend fun countCommit(
        businessId: Int,
        idempotencyKey: String,
        locationId: Int,
        itemId: Int,
        countedQty: String,
    ): ApiResponse {
        val res = client.post("${Configs.serverBasePath}inventory/count-commit") {
            authHeaders(businessId)
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("business_id", businessId)
                put("idempotency_key", idempotencyKey)
                put("location_id", locationId)
                put("lines", buildJsonArray {
                    add(buildJsonObject {
                        put("item_id", itemId)
                        put("counted_qty", countedQty)
                    })
                })
            })
        }
        return parse(res.status, res.body(), retry = {
            countCommit(businessId, idempotencyKey, locationId, itemId, countedQty)
        })
    }

    suspend fun belowMin(businessId: Int, locationId: Int?): ApiResponse {
        val res = client.post("${Configs.serverBasePath}inventory/below-min") {
            authHeaders(businessId)
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("business_id", businessId)
                if (locationId != null && locationId > 0) {
                    put("location_id", locationId)
                }
            })
        }
        return parse(res.status, res.body(), retry = { belowMin(businessId, locationId) })
    }

    suspend fun dashboard(businessId: Int): ApiResponse {
        val res = client.post("${Configs.serverBasePath}inventory/dashboard") {
            authHeaders(businessId)
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("business_id", businessId) })
        }
        return parse(res.status, res.body(), retry = { dashboard(businessId) })
    }

    suspend fun itemProfile(businessId: Int, itemId: Int): ApiResponse {
        val res = client.post("${Configs.serverBasePath}inventory/item-profile") {
            authHeaders(businessId)
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("business_id", businessId)
                put("item_id", itemId)
            })
        }
        return parse(res.status, res.body(), retry = { itemProfile(businessId, itemId) })
    }

    suspend fun updateItemProfile(
        businessId: Int,
        itemId: Int,
        tracked: Boolean,
        allowNegativeOverride: Boolean?,
        expectedVersion: Int,
        minQty: String?,
        reorderQty: String?,
    ): ApiResponse {
        val res = client.post("${Configs.serverBasePath}inventory/update-item-profile") {
            authHeaders(businessId)
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("business_id", businessId)
                put("item_id", itemId)
                put("is_inventory_tracked", tracked)
                if (allowNegativeOverride == null) {
                    put("allow_negative_stock_override", JsonNull)
                } else {
                    put("allow_negative_stock_override", allowNegativeOverride)
                }
                put("expected_version", expectedVersion)
                if (!minQty.isNullOrBlank()) put("min_qty", minQty)
                if (!reorderQty.isNullOrBlank()) put("reorder_qty", reorderQty)
            })
        }
        return parse(res.status, res.body(), retry = {
            updateItemProfile(businessId, itemId, tracked, allowNegativeOverride, expectedVersion, minQty, reorderQty)
        })
    }

    suspend fun balances(businessId: Int, itemId: Int, stockState: String? = null): ApiResponse {
        val res = client.post("${Configs.serverBasePath}inventory/balances") {
            authHeaders(businessId)
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("business_id", businessId)
                put("item_id", itemId)
                if (!stockState.isNullOrBlank()) {
                    put("stock_state", stockState)
                }
            })
        }
        return parse(res.status, res.body(), retry = { balances(businessId, itemId, stockState) })
    }

    suspend fun kardex(
        businessId: Int,
        itemId: Int,
        limit: Int = InventoryKardexSupport.KARDEX_FETCH_LIMIT,
        beforeMovementId: Int? = null,
    ): ApiResponse {
        val res = client.post("${Configs.serverBasePath}inventory/kardex") {
            authHeaders(businessId)
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("business_id", businessId)
                put("item_id", itemId)
                put("limit", limit)
                if (beforeMovementId != null && beforeMovementId > 0) {
                    put("before_movement_id", beforeMovementId)
                }
            })
        }
        return parse(res.status, res.body(), retry = { kardex(businessId, itemId, limit, beforeMovementId) })
    }

    suspend fun valuation(businessId: Int, itemId: Int): ApiResponse {
        val res = client.post("${Configs.serverBasePath}inventory/valuation") {
            authHeaders(businessId)
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("business_id", businessId)
                put("item_id", itemId)
            })
        }
        return parse(res.status, res.body(), retry = { valuation(businessId, itemId) })
    }

    suspend fun balancesLegacyAvailableOnly(businessId: Int, itemId: Int): ApiResponse =
        balances(businessId, itemId, stockState = "available")

    private fun io.ktor.client.request.HttpRequestBuilder.authHeaders(businessId: Int) {
        headers {
            append(HttpHeaders.Accept, "*/*")
            append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
            append("X-Business-ID", "$businessId")
            append(HttpHeaders.ContentType, "application/json")
        }
    }

    private suspend fun parse(
        status: HttpStatusCode,
        body: JsonObject,
        retry: suspend () -> ApiResponse,
    ): ApiResponse {
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001 || status == HttpStatusCode.Unauthorized) {
            return try {
                authService.refreshToken(client)
                retry()
            } catch (_: Exception) {
                response
            }
        }
        return response
    }
}
