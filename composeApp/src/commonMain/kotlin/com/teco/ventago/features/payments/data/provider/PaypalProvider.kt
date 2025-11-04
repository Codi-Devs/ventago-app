package com.teco.ventago.features.payments.data.provider

import com.teco.ventago.Configs
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.utils.ApiError
import com.teco.ventago.utils.ApiResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonObject

interface IPaypalProvider {
    suspend fun connect(businessId: Int): ApiResponse
    suspend fun createBillingAgreement(businessId: Int): ApiResponse
    suspend fun unlink(businessId: Int): ApiResponse
}

class PaypalProvider(private val client: HttpClient, private val authService: IAuthService) :
    IPaypalProvider {
    override suspend fun connect(businessId: Int): ApiResponse {
        val res = client.get(Configs.ordersBasePath + "/api/v1/business/payment-methods/paypal/connect") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                connect(businessId)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun createBillingAgreement(businessId: Int): ApiResponse {
        val res =
            client.post(Configs.ordersBasePath + "/api/v1/business/billing/paypal/billing-agreement/create") {
                headers {
                    append(HttpHeaders.Accept, "*/*")
                    append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                    append(HttpHeaders.ContentType, "application/json")
                    append("X-Business-ID", "$businessId")
                }
                contentType(ContentType.Application.Json)
                setBody(PaypalRequests.businessIdRequest(businessId))
            }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                createBillingAgreement(businessId)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun unlink(businessId: Int): ApiResponse {
        val res = client.delete(Configs.ordersBasePath + "/api/v1/business/payment-methods/paypal/unlink") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
            setBody(PaypalRequests.businessIdRequest(businessId))
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                unlink(businessId)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }
}