package com.teco.ventago.features.notifications.data.provider

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
import kotlinx.serialization.json.buildJsonObject

interface INotificationsProvider {
    suspend fun listNotifications(businessId: Int, limit: Int, offset: Int): ApiResponse
    suspend fun unreadCount(businessId: Int): ApiResponse
    suspend fun markSeen(businessId: Int, notificationId: Long): ApiResponse
    suspend fun dismiss(businessId: Int, notificationId: Long): ApiResponse
    suspend fun remove(businessId: Int, notificationId: Long): ApiResponse
}

class NotificationsProvider(
    private val client: HttpClient,
    private val authService: IAuthService
) : INotificationsProvider {

    override suspend fun listNotifications(businessId: Int, limit: Int, offset: Int): ApiResponse {
        val res = client.get(Configs.ordersBasePath + "/api/v1/notifications") {
            url {
                parameters.append("limit", limit.toString())
                parameters.append("offset", offset.toString())
            }
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
                listNotifications(businessId, limit, offset)
            } catch (_: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun unreadCount(businessId: Int): ApiResponse {
        val res = client.get(Configs.ordersBasePath + "/api/v1/notifications/unread-count") {
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
                unreadCount(businessId)
            } catch (_: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun markSeen(businessId: Int, notificationId: Long): ApiResponse {
        val res = client.post(Configs.ordersBasePath + "/api/v1/notifications/$notificationId/seen") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { })
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                markSeen(businessId, notificationId)
            } catch (_: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun dismiss(businessId: Int, notificationId: Long): ApiResponse {
        val res = client.post(Configs.ordersBasePath + "/api/v1/notifications/$notificationId/dismiss") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { })
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                dismiss(businessId, notificationId)
            } catch (_: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun remove(businessId: Int, notificationId: Long): ApiResponse {
        val res = client.delete(Configs.ordersBasePath + "/api/v1/notifications/$notificationId") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { })
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                remove(businessId, notificationId)
            } catch (_: Exception) {
                response
            }
        }
        return response
    }
}
