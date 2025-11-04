package com.teco.ventago.features.payments.data.provider

import com.teco.ventago.Configs
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.utils.ApiError
import com.teco.ventago.utils.ApiResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonObject


interface IPaymentsProvider {
    suspend fun onboardPayments(businessId: Int): ApiResponse
    suspend fun transference(businessId: Int, enabled: Boolean, instructions: String): ApiResponse
}

class PaymentsProvider (private val client: HttpClient, private val authService: IAuthService): IPaymentsProvider {

    override suspend fun onboardPayments(businessId: Int): ApiResponse {
        val res = client.post(Configs.ordersBasePath+"/api/v1/business/payments/onboard") {
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
                onboardPayments(businessId)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun transference(businessId: Int, enabled: Boolean, instructions: String): ApiResponse {
        val res = client.put(Configs.ordersBasePath+"/api/v1/business/payment-methods/transference") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
            setBody(PaypalRequests.transference(businessId, enabled, instructions))
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                transference(businessId, enabled, instructions)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

}