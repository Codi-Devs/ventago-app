package com.teco.ventago.features.pos.devices.data.provider

import com.teco.ventago.Configs
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.pos.devices.domain.model.PosDevicesListRequest
import com.teco.ventago.features.pos.devices.domain.model.UpdatePosDevicePermissionsRequest
import com.teco.ventago.features.pos.devices.domain.model.UpdatePosDeviceRequest
import com.teco.ventago.utils.ApiError
import com.teco.ventago.utils.ApiResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

class PosDevicesProvider(
    private val client: HttpClient,
    private val authService: IAuthService,
) : IPosDevicesProvider {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    override suspend fun listDevices(businessId: Int, request: PosDevicesListRequest): ApiResponse {
        val response = client.get("${Configs.ordersBasePath}/api/v1/devices") {
            appendBusinessHeaders(businessId)
            parameter("page", request.page)
            parameter("page_size", request.pageSize)
            request.status?.takeIf { it.isNotBlank() }?.let { parameter("status", it) }
        }.body<JsonObject>().let(ApiResponse::fromJson)

        return if (response.error == ApiError.AUTH_001) {
            try {
                authService.refreshToken(client)
                listDevices(businessId, request)
            } catch (_: Exception) {
                response
            }
        } else {
            response
        }
    }

    override suspend fun getPosConfig(deviceId: String): ApiResponse {
        val response = client.get("${Configs.ordersBasePath}/api/v1/devices/$deviceId/pos-config") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
            }
        }.body<JsonObject>().let(ApiResponse::fromJson)

        return if (response.error == ApiError.AUTH_001) {
            try {
                authService.refreshToken(client)
                getPosConfig(deviceId)
            } catch (_: Exception) {
                response
            }
        } else {
            response
        }
    }

    override suspend fun updateDevice(
        businessId: Int,
        deviceId: String,
        request: UpdatePosDeviceRequest,
    ): ApiResponse {
        val response = client.patch("${Configs.ordersBasePath}/api/v1/devices/$deviceId") {
            appendBusinessHeaders(businessId)
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(UpdatePosDeviceRequest.serializer(), request))
        }.body<JsonObject>().let(ApiResponse::fromJson)

        return if (response.error == ApiError.AUTH_001) {
            try {
                authService.refreshToken(client)
                updateDevice(businessId, deviceId, request)
            } catch (_: Exception) {
                response
            }
        } else {
            response
        }
    }

    override suspend fun updatePermissions(
        businessId: Int,
        deviceId: String,
        request: UpdatePosDevicePermissionsRequest,
    ): ApiResponse {
        val response = client.put("${Configs.ordersBasePath}/api/v1/devices/$deviceId/permissions") {
            appendBusinessHeaders(businessId)
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(UpdatePosDevicePermissionsRequest.serializer(), request))
        }.body<JsonObject>().let(ApiResponse::fromJson)

        return if (response.error == ApiError.AUTH_001) {
            try {
                authService.refreshToken(client)
                updatePermissions(businessId, deviceId, request)
            } catch (_: Exception) {
                response
            }
        } else {
            response
        }
    }

    private fun HttpRequestBuilder.appendBusinessHeaders(businessId: Int) {
        headers {
            append(HttpHeaders.Accept, "*/*")
            append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
            append(HttpHeaders.ContentType, "application/json")
            append("X-Business-ID", businessId.toString())
        }
    }
}
