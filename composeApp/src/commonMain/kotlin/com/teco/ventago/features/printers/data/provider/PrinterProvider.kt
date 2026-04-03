package com.teco.ventago.features.printers.data.provider

import com.teco.ventago.Configs
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.printers.domain.model.CreatePrinterRequest
import com.teco.ventago.features.printers.domain.model.PrinterListFilters
import com.teco.ventago.features.printers.domain.model.UpdatePrinterRequest
import com.teco.ventago.utils.ApiError
import com.teco.ventago.utils.ApiResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

class PrinterProvider(
    private val client: HttpClient,
    private val authService: IAuthService,
) : IPrinterProvider {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    override suspend fun createPrinter(businessId: Int, request: CreatePrinterRequest): ApiResponse {
        val response = client.post("${Configs.ordersBasePath}/api/v1/printers") {
            appendBusinessHeaders(businessId)
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(request))
        }.body<JsonObject>().let(ApiResponse::fromJson)

        return if (response.error == ApiError.AUTH_001) {
            try {
                authService.refreshToken(client)
                createPrinter(businessId, request)
            } catch (_: Exception) {
                response
            }
        } else {
            response
        }
    }

    override suspend fun listPrinters(businessId: Int, filters: PrinterListFilters): ApiResponse {
        val response = client.get("${Configs.ordersBasePath}/api/v1/printers") {
            appendBusinessHeaders(businessId)
            filters.branchCode?.takeIf { it.isNotBlank() }?.let { parameter("branch_code", it) }
            filters.billingPointCode?.takeIf { it.isNotBlank() }?.let { parameter("billing_point_code", it) }
            filters.isActive?.let { parameter("is_active", it) }
            filters.printByDefault?.let { parameter("print_by_default", it) }
        }.body<JsonObject>().let(ApiResponse::fromJson)

        return if (response.error == ApiError.AUTH_001) {
            try {
                authService.refreshToken(client)
                listPrinters(businessId, filters)
            } catch (_: Exception) {
                response
            }
        } else {
            response
        }
    }

    override suspend fun getPrinter(businessId: Int, branchCode: String, billingPointCode: String): ApiResponse {
        val response = client.get(
            "${Configs.ordersBasePath}/api/v1/printers/branches/$branchCode/billing-points/$billingPointCode"
        ) {
            appendBusinessHeaders(businessId)
        }.body<JsonObject>().let(ApiResponse::fromJson)

        return if (response.error == ApiError.AUTH_001) {
            try {
                authService.refreshToken(client)
                getPrinter(businessId, branchCode, billingPointCode)
            } catch (_: Exception) {
                response
            }
        } else {
            response
        }
    }

    override suspend fun updatePrinter(
        businessId: Int,
        branchCode: String,
        billingPointCode: String,
        request: UpdatePrinterRequest,
    ): ApiResponse {
        val response = client.put(
            "${Configs.ordersBasePath}/api/v1/printers/branches/$branchCode/billing-points/$billingPointCode"
        ) {
            appendBusinessHeaders(businessId)
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(request))
        }.body<JsonObject>().let(ApiResponse::fromJson)

        return if (response.error == ApiError.AUTH_001) {
            try {
                authService.refreshToken(client)
                updatePrinter(businessId, branchCode, billingPointCode, request)
            } catch (_: Exception) {
                response
            }
        } else {
            response
        }
    }

    override suspend fun deletePrinter(businessId: Int, branchCode: String, billingPointCode: String): ApiResponse {
        val response = client.delete(
            "${Configs.ordersBasePath}/api/v1/printers/branches/$branchCode/billing-points/$billingPointCode"
        ) {
            appendBusinessHeaders(businessId)
        }.body<JsonObject>().let(ApiResponse::fromJson)

        return if (response.error == ApiError.AUTH_001) {
            try {
                authService.refreshToken(client)
                deletePrinter(businessId, branchCode, billingPointCode)
            } catch (_: Exception) {
                response
            }
        } else {
            response
        }
    }

    override suspend fun getOrderTicketLayout(businessId: Int, orderId: Int): ApiResponse {
        val response = client.get("${Configs.ordersBasePath}/api/v1/orders/$orderId/invoices/docs/ticket") {
            appendBusinessHeaders(businessId)
        }.body<JsonObject>().let(ApiResponse::fromJson)

        return if (response.error == ApiError.AUTH_001) {
            try {
                authService.refreshToken(client)
                getOrderTicketLayout(businessId, orderId)
            } catch (_: Exception) {
                response
            }
        } else {
            response
        }
    }

    private fun io.ktor.client.request.HttpRequestBuilder.appendBusinessHeaders(businessId: Int) {
        headers {
            append(HttpHeaders.Accept, "*/*")
            append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
            append(HttpHeaders.ContentType, "application/json")
            append("X-Business-ID", businessId.toString())
        }
    }
}

