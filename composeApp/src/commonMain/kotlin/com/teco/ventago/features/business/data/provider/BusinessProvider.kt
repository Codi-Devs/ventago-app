package com.teco.ventago.features.business.data.provider

import com.teco.ventago.Configs
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.business.domain.model.requests.RegisterBusinessRequest
import com.teco.ventago.utils.ApiError
import com.teco.ventago.utils.ApiResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive

class BusinessProvider(private val client: HttpClient, private val authService: IAuthService) :
    IBusinessProvider {

    override suspend fun updateWebStyle(
        businessId: Int,
        styleId: Int,
        primaryColor: String,
        secondaryColor: String,
    ): ApiResponse {
        val res = client.post(Configs.serverBasePath + "bussiness/update-web-style") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
            }
            contentType(ContentType.Application.Json)
            setBody(
                BusinessRequests.updateWebStyle(
                    businessId,
                    styleId,
                    primaryColor,
                    secondaryColor
                )
            )
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                updateWebStyle(businessId, styleId, primaryColor, secondaryColor)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun resetColors(businessId: Int): ApiResponse {
        val res = client.post(Configs.serverBasePath + "bussiness/reset-colors") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
            }
            contentType(ContentType.Application.Json)
            setBody(
                BusinessRequests.resetColors(
                    businessId
                )
            )
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                resetColors(businessId)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun registerBusiness(request: RegisterBusinessRequest): ApiResponse {
        val res = client.post(Configs.serverBasePath + "business/register") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
            }
            contentType(ContentType.Application.Json)
            setBody(
                BusinessRequests.register(
                    request
                )
            )
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                registerBusiness(request)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun getBusinessesByUser(): ApiResponse {
        val res = client.post(Configs.serverBasePath + "business/get-businesses-by-user") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
            }
            contentType(ContentType.Application.Json)
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                getBusinessesByUser()
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun checkDomain(domain: String): ApiResponse {
        val res = client.post(Configs.serverBasePath + "bussiness/check-domain") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.ContentType, "application/json")
            }
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                    "domain": "$domain"
                }
            """.trimIndent()
            )
        }

        if (!res.status.isSuccess()) {
            return ApiResponse(false, JsonPrimitive(false), ApiError.NO_ERROR)
        }

        val body = res.body<JsonObject>()
        return if (body["status"]?.jsonPrimitive?.booleanOrNull == true) {
            ApiResponse(true, JsonPrimitive(true), ApiError.NO_ERROR)
        } else {
            ApiResponse(false, JsonPrimitive(false), ApiError.NO_ERROR)
        }
    }


    override suspend fun getBusinessById(businessId: Int): ApiResponse {
        return requestBusinessById(businessId, allowRetry = true)
    }

    private suspend fun requestBusinessById(businessId: Int, allowRetry: Boolean): ApiResponse {
        val accessToken = authService.getJwtToken()
        val res = client.post(Configs.serverBasePath + "business/get-business-by-id") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer $accessToken")
                append(HttpHeaders.ContentType, "application/json")
            }
            contentType(ContentType.Application.Json)
            setBody(
                BusinessRequests.getBusinessById(
                    businessId
                )
            )
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        val shouldRefreshToken = response.error == ApiError.AUTH_001 || res.status == HttpStatusCode.Unauthorized
        if (allowRetry && shouldRefreshToken) {
            return try {
                authService.refreshToken(client, failedAccessToken = accessToken)
                requestBusinessById(businessId, allowRetry = false)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun removeBusiness(businessId: Int): ApiResponse {
        val res = client.post(Configs.serverBasePath + "business/remove-business") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
            }
            contentType(ContentType.Application.Json)
            setBody(
                BusinessRequests.getBusinessById(
                    businessId
                )
            )
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                removeBusiness(businessId)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }


}
