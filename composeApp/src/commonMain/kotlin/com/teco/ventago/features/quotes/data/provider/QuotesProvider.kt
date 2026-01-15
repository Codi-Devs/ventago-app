package com.teco.ventago.features.quotes.data.provider

import com.teco.ventago.Configs
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.quotes.domain.models.requests.CancelQuoteRequest
import com.teco.ventago.features.quotes.domain.models.requests.CreateQuoteRequest
import com.teco.ventago.features.quotes.domain.models.requests.GetQuoteRequest
import com.teco.ventago.features.quotes.domain.models.requests.ListQuotesRequest
import com.teco.ventago.features.quotes.domain.models.requests.SendQuoteEmailRequest
import com.teco.ventago.features.quotes.domain.models.requests.UpdateQuoteRequest
import com.teco.ventago.features.quotes.domain.models.QuoteSettings
import com.teco.ventago.json
import com.teco.ventago.utils.ApiError
import com.teco.ventago.utils.ApiResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
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

class QuotesProvider(
    private val client: HttpClient,
    private val authService: IAuthService
) : IQuotesProvider {

    override suspend fun listQuotes(businessId: Int, request: ListQuotesRequest): ApiResponse {
        val res = client.post(Configs.ordersBasePath + "/api/v1/quotes/list") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(request))
        }
        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001 || res.status == HttpStatusCode.Unauthorized) {
            return try {
                authService.refreshToken(client)
                listQuotes(businessId, request)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun getQuote(businessId: Int, request: GetQuoteRequest): ApiResponse {
        val res = client.post(Configs.ordersBasePath + "/api/v1/quotes/get") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(request))
        }
        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001 || res.status == HttpStatusCode.Unauthorized) {
            return try {
                authService.refreshToken(client)
                getQuote(businessId, request)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun getQuotePdf(businessId: Int, quoteId: Long): ApiResponse {
        val res = client.get(Configs.ordersBasePath + "/api/v1/quotes/$quoteId/pdf") {
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
        if (response.error == ApiError.AUTH_001 || res.status == HttpStatusCode.Unauthorized) {
            return try {
                authService.refreshToken(client)
                getQuotePdf(businessId, quoteId)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun sendQuoteEmail(businessId: Int, request: SendQuoteEmailRequest): ApiResponse {
        val res = client.post(Configs.ordersBasePath + "/api/v1/quotes/send-email") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(request))
        }
        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001 || res.status == HttpStatusCode.Unauthorized) {
            return try {
                authService.refreshToken(client)
                sendQuoteEmail(businessId, request)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun cancelQuote(businessId: Int, request: CancelQuoteRequest): ApiResponse {
        val res = client.post(Configs.ordersBasePath + "/api/v1/quotes/cancel") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(request))
        }
        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001 || res.status == HttpStatusCode.Unauthorized) {
            return try {
                authService.refreshToken(client)
                cancelQuote(businessId, request)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun createQuote(businessId: Int, request: CreateQuoteRequest): ApiResponse {
        val res = client.post(Configs.ordersBasePath + "/api/v1/quotes/create") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(request))
        }
        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001 || res.status == HttpStatusCode.Unauthorized) {
            return try {
                authService.refreshToken(client)
                createQuote(businessId, request)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun updateQuote(businessId: Int, request: UpdateQuoteRequest): ApiResponse {
        val res = client.post(Configs.ordersBasePath + "/api/v1/quotes/update") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(request))
        }
        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001 || res.status == HttpStatusCode.Unauthorized) {
            return try {
                authService.refreshToken(client)
                updateQuote(businessId, request)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun getQuoteSettings(businessId: Int): ApiResponse {
        val res = client.get(Configs.ordersBasePath + "/api/v1/quotes/settings") {
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
        if (response.error == ApiError.AUTH_001 || res.status == HttpStatusCode.Unauthorized) {
            return try {
                authService.refreshToken(client)
                getQuoteSettings(businessId)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun updateQuoteSettings(businessId: Int, request: QuoteSettings): ApiResponse {
        val res = client.put(Configs.ordersBasePath + "/api/v1/quotes/settings") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(request))
        }
        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001 || res.status == HttpStatusCode.Unauthorized) {
            return try {
                authService.refreshToken(client)
                updateQuoteSettings(businessId, request)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }
}
