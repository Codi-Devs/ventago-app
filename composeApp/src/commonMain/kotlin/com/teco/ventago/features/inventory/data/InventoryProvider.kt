package com.teco.ventago.features.inventory.data

import com.teco.ventago.Configs
import com.teco.ventago.features.auth.domain.IAuthService
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
