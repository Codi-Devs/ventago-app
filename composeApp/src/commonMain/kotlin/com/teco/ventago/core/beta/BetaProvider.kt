package com.teco.ventago.core.beta

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
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.Json

class BetaProvider(
    private val client: HttpClient,
    private val authService: IAuthService,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    suspend fun listFeatures(businessId: Int): ApiResponse {
        val res = client.get(Configs.ordersBasePath + "/api/v1/beta/features") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append("X-Business-ID", "$businessId")
                append(HttpHeaders.ContentType, "application/json")
            }
            contentType(ContentType.Application.Json)
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        println("ASDASD: response betas: ${response.toJson()}")
        if (response.error == ApiError.AUTH_001 || res.status == HttpStatusCode.Unauthorized) {
            return try {
                authService.refreshToken(client)
                listFeatures(businessId)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }
}
