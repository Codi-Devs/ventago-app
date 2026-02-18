package com.teco.ventago.features.home.data.provider

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
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

class HomeSummaryProvider(
    private val client: HttpClient,
    private val authService: IAuthService
) : IHomeSummaryProvider {

    override suspend fun getHomeSummary(businessId: Int): ApiResponse {
        return requestHomeSummary(businessId, allowRetry = true)
    }

    private suspend fun requestHomeSummary(businessId: Int, allowRetry: Boolean): ApiResponse {
        val res = client.get(Configs.ordersBasePath + "/api/v1/home/summary") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
        }

        val body = res.body<JsonObject>()
        val response = normalizeHomeSummaryApiResponse(body)
        val shouldRefreshToken = response.error == ApiError.AUTH_001 || res.status == HttpStatusCode.Unauthorized

        if (allowRetry && shouldRefreshToken) {
            return try {
                authService.refreshToken(client)
                requestHomeSummary(businessId, allowRetry = false)
            } catch (_: Exception) {
                response
            }
        }

        return response
    }
}

fun normalizeHomeSummaryApiResponse(body: JsonObject): ApiResponse {
    val hasWrappedShape = body.containsKey("data") && (
        body.containsKey("error") ||
            body.containsKey("message") ||
            body.containsKey("timestamp") ||
            body.containsKey("success")
        )

    if (!hasWrappedShape) {
        return ApiResponse(successful = true, data = body, error = ApiError.NO_ERROR)
    }

    val errorCode = body["error"]?.jsonPrimitive?.contentOrNull
    val isSuccessful = body["success"]?.jsonPrimitive?.booleanOrNull ?: errorCode.isNullOrBlank()

    return ApiResponse(
        successful = isSuccessful,
        data = body["data"],
        error = ApiError.fromError(errorCode)
    )
}
