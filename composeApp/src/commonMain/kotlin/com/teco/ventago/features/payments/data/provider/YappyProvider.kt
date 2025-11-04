package com.teco.ventago.features.payments.data.provider

import com.teco.ventago.Configs
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.utils.ApiError
import com.teco.ventago.utils.ApiResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonObject

interface IYappyProvider {
    suspend fun connect(businessId: Int, merchantID: String, domain: String, secretKey: String): ApiResponse
    suspend fun unlink(businessId: Int): ApiResponse
}

class YappyProvider(private val client: HttpClient, private val authService: IAuthService) :
    IYappyProvider {
    override suspend fun connect(businessId: Int, merchantID: String, domain: String, secretKey: String): ApiResponse {
        val res = client.put(Configs.ordersBasePath + "/api/v1/business/payment-methods/yappy/link") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
            setBody(PaypalRequests.yappyConnect(businessId, merchantID, domain, secretKey))
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                connect(businessId, merchantID, domain, secretKey)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun unlink(businessId: Int): ApiResponse {
        val res = client.delete(Configs.ordersBasePath + "/api/v1/business/payment-methods/yappy/unlink") {
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