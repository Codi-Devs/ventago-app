package com.teco.ventago.features.user.data.repository

import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.features.auth.data.provider.IAuthProvider
import com.teco.ventago.features.auth.domain.model.response.AuthResponse
import com.teco.ventago.features.user.data.provider.IUserProvider
import com.teco.ventago.utils.BadRequestException
import com.teco.ventago.utils.isError
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.decodeFromJsonElement

class UserRepository(private val provider: IAuthProvider, private val userProvider: IUserProvider, private val logger: ILoggerService):
    IUserRepository {
    override suspend fun setPremium(premium: Boolean): Boolean {

        return true
    }

    override suspend fun deleteAccount(token: String): Boolean {
        return try {
            val response = provider.deleteAccount(token)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            if (response.data is JsonPrimitive) {
                response.data.boolean
            } else {
                false
            }
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR, "deleteAccount",
                    "Error deleting account. Error: ${e.message ?: "UNKNOWN" }"
                )
            )
            throw e
        }
    }

    override suspend fun getUserData(token: String): AuthResponse {
        try {
            val response = userProvider.getUserData(token)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            if (response.data is JsonObject) {
                val map = Json.decodeFromJsonElement<Map<String, JsonElement>>(response.data)
                return AuthResponse.fromMap(map)
            } else {
                throw BadRequestException(response.toJson())
            }
        } catch (e: Exception) {
            if (e.message?.contains("AUTH_001") == true) {
                throw e
            }
            logger.sendLog(
                Log(
                    LogLevel.ERROR, "getUserData",
                    "Error getUserData account. Error: ${e.message ?: "UNKNOWN" }"
                )
            )
            throw e
        }
    }

}