package com.teco.ventago.features.invoicing.data.provider

import com.teco.ventago.Configs
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.invoicing.domain.models.BottomNoteSettingsRequest
import com.teco.ventago.features.invoicing.domain.models.IncludeAddressOnInvoiceRequest
import com.teco.ventago.json
import com.teco.ventago.utils.ApiError
import com.teco.ventago.utils.ApiResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject

class InvoicingSettingsProvider(
    private val client: HttpClient,
    private val authService: IAuthService,
) : IInvoicingSettingsProvider {
    override suspend fun getInvoicingSettings(businessId: Int): ApiResponse {
        val res = client.get(SETTINGS_URL) {
            invoicingSettingsHeaders(businessId)
            contentType(ContentType.Application.Json)
        }
        val response = ApiResponse.fromJson(res.body<JsonObject>())
        if (response.error == ApiError.AUTH_001 || res.status == HttpStatusCode.Unauthorized) {
            return try {
                authService.refreshToken(client)
                getInvoicingSettings(businessId)
            } catch (_: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun updateIncludeAddressOnInvoice(
        businessId: Int,
        request: IncludeAddressOnInvoiceRequest,
    ): ApiResponse {
        val res = client.put(INCLUDE_ADDRESS_URL) {
            invoicingSettingsHeaders(businessId)
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(IncludeAddressOnInvoiceRequest.serializer(), request))
        }
        val response = ApiResponse.fromJson(res.body<JsonObject>())
        if (response.error == ApiError.AUTH_001 || res.status == HttpStatusCode.Unauthorized) {
            return try {
                authService.refreshToken(client)
                updateIncludeAddressOnInvoice(businessId, request)
            } catch (_: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun getBottomNoteSettings(businessId: Int): ApiResponse {
        val res = client.get(BOTTOM_NOTE_URL) {
            invoicingSettingsHeaders(businessId)
            contentType(ContentType.Application.Json)
        }
        val response = ApiResponse.fromJson(res.body<JsonObject>())
        if (response.error == ApiError.AUTH_001 || res.status == HttpStatusCode.Unauthorized) {
            return try {
                authService.refreshToken(client)
                getBottomNoteSettings(businessId)
            } catch (_: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun createBottomNoteSettings(
        businessId: Int,
        request: BottomNoteSettingsRequest,
    ): ApiResponse {
        val res = client.post(BOTTOM_NOTE_URL) {
            invoicingSettingsHeaders(businessId)
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(BottomNoteSettingsRequest.serializer(), request))
        }
        val response = ApiResponse.fromJson(res.body<JsonObject>())
        if (response.error == ApiError.AUTH_001 || res.status == HttpStatusCode.Unauthorized) {
            return try {
                authService.refreshToken(client)
                createBottomNoteSettings(businessId, request)
            } catch (_: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun updateBottomNoteSettings(
        businessId: Int,
        request: BottomNoteSettingsRequest,
    ): ApiResponse {
        val res = client.put(BOTTOM_NOTE_URL) {
            invoicingSettingsHeaders(businessId)
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(BottomNoteSettingsRequest.serializer(), request))
        }
        val response = ApiResponse.fromJson(res.body<JsonObject>())
        if (response.error == ApiError.AUTH_001 || res.status == HttpStatusCode.Unauthorized) {
            return try {
                authService.refreshToken(client)
                updateBottomNoteSettings(businessId, request)
            } catch (_: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun deleteBottomNoteSettings(businessId: Int): ApiResponse {
        val res = client.delete(BOTTOM_NOTE_URL) {
            invoicingSettingsHeaders(businessId)
            contentType(ContentType.Application.Json)
        }
        val response = ApiResponse.fromJson(res.body<JsonObject>())
        if (response.error == ApiError.AUTH_001 || res.status == HttpStatusCode.Unauthorized) {
            return try {
                authService.refreshToken(client)
                deleteBottomNoteSettings(businessId)
            } catch (_: Exception) {
                response
            }
        }
        return response
    }

    private fun io.ktor.client.request.HttpRequestBuilder.invoicingSettingsHeaders(businessId: Int) {
        headers {
            append(HttpHeaders.Accept, "*/*")
            append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
            append(HttpHeaders.ContentType, "application/json")
            append("X-Business-ID", "$businessId")
        }
    }

    private companion object {
        val SETTINGS_URL = "${Configs.ordersBasePath}/api/v1/invoicing/settings"
        val BOTTOM_NOTE_URL = "${Configs.ordersBasePath}/api/v1/invoicing/settings/bottom-note"
        val INCLUDE_ADDRESS_URL = "${Configs.ordersBasePath}/api/v1/invoicing/settings/include-address"
    }
}
