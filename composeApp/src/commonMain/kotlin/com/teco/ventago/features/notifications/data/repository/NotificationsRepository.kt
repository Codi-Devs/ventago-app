package com.teco.ventago.features.notifications.data.repository

import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.features.notifications.data.provider.INotificationsProvider
import com.teco.ventago.features.notifications.domain.models.NotificationsPage
import com.teco.ventago.features.notifications.domain.models.NotificationsUnreadCount
import com.teco.ventago.utils.ApiResponse
import com.teco.ventago.utils.BadRequestException
import com.teco.ventago.utils.isError
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive

interface INotificationsRepository {
    suspend fun listNotifications(businessId: Int, limit: Int, offset: Int): NotificationsPage
    suspend fun unreadCount(businessId: Int): Int
    suspend fun markSeen(businessId: Int, notificationId: Long): Boolean
    suspend fun dismiss(businessId: Int, notificationId: Long): Boolean
    suspend fun remove(businessId: Int, notificationId: Long): Boolean
}

class NotificationsRepository(
    private val provider: INotificationsProvider,
    private val logger: ILoggerService
) : INotificationsRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun listNotifications(businessId: Int, limit: Int, offset: Int): NotificationsPage {
        return try {
            val response = provider.listNotifications(businessId, limit, offset)
            ensureOk(response)
            val data = response.data as? JsonObject ?: JsonObject(emptyMap())
            json.decodeFromJsonElement<NotificationsPage>(data)
        } catch (e: Exception) {
            logger.sendLog(Log(LogLevel.ERROR, "NotificationsRepository::listNotifications", "Error listing notifications. Error: ${e.message ?: "UNKNOWN"}"))
            throw e
        }
    }

    override suspend fun unreadCount(businessId: Int): Int {
        return try {
            val response = provider.unreadCount(businessId)
            ensureOk(response)
            val data = response.data as? JsonObject ?: JsonObject(emptyMap())
            json.decodeFromJsonElement<NotificationsUnreadCount>(data).unreadCount
        } catch (e: Exception) {
            logger.sendLog(Log(LogLevel.ERROR, "NotificationsRepository::unreadCount", "Error loading unread notifications count. Error: ${e.message ?: "UNKNOWN"}"))
            throw e
        }
    }

    override suspend fun markSeen(businessId: Int, notificationId: Long): Boolean {
        return mutate("markSeen", businessId, notificationId) {
            provider.markSeen(businessId, notificationId)
        }
    }

    override suspend fun dismiss(businessId: Int, notificationId: Long): Boolean {
        return mutate("dismiss", businessId, notificationId) {
            provider.dismiss(businessId, notificationId)
        }
    }

    override suspend fun remove(businessId: Int, notificationId: Long): Boolean {
        return mutate("remove", businessId, notificationId) {
            provider.remove(businessId, notificationId)
        }
    }

    private suspend fun mutate(
        action: String,
        businessId: Int,
        notificationId: Long,
        call: suspend () -> ApiResponse
    ): Boolean {
        return try {
            val response = call()
            ensureOk(response)
            response.successful || runCatching { response.data?.jsonPrimitive?.booleanOrNull }.getOrNull() == true
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR,
                    "NotificationsRepository::$action",
                    "Error on notifications $action. Error: ${e.message ?: "UNKNOWN"}, businessId: $businessId, notificationId: $notificationId"
                )
            )
            throw e
        }
    }

    private fun ensureOk(response: ApiResponse) {
        if (response.error.isError()) {
            throw BadRequestException(response.toJson())
        }
    }
}
