package com.teco.ventago.features.pos.provisioning.data.provider

import com.teco.ventago.Configs
import com.teco.ventago.utils.ApiResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonObject

interface IPosDeviceProvisioningProvider {
    suspend fun getPosConfig(deviceId: String, accessToken: String?): ApiResponse
}

class PosDeviceProvisioningProvider(
    private val client: HttpClient,
) : IPosDeviceProvisioningProvider {
    override suspend fun getPosConfig(deviceId: String, accessToken: String?): ApiResponse {
        val res = client.get("${Configs.ordersBasePath}/api/v1/devices/$deviceId/pos-config") {
            contentType(ContentType.Application.Json)
            if (!accessToken.isNullOrBlank()) {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $accessToken")
                }
            }
        }
        return ApiResponse.fromJson(res.body<JsonObject>())
    }
}
