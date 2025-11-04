package com.teco.ventago.features.financialProfile.data.provider

import com.teco.ventago.Configs
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.utils.ApiError
import com.teco.ventago.utils.ApiResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonObject

interface IFinancialProfileProvider {
    suspend fun getFinancialProfile(businessId: Int): ApiResponse
}

class FinancialProfileProvider(private val client: HttpClient, private val authService: IAuthService): IFinancialProfileProvider {

    override suspend fun getFinancialProfile(businessId: Int): ApiResponse {
        val res = client.get(Configs.ordersBasePath+"/api/v1/business/config-summary") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
        }

        val body = res.body<JsonObject>()
        println("ASDASD: Response from backend: $body")
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                getFinancialProfile(businessId)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }
}