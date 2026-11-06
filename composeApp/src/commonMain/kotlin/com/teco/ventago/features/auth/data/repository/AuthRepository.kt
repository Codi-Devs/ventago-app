package com.teco.ventago.features.auth.data.repository

import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.features.auth.data.provider.IAuthProvider
import com.teco.ventago.features.auth.domain.model.requests.CreateUserRequest
import com.teco.ventago.features.auth.domain.model.requests.EmailLoginRequest
import com.teco.ventago.features.auth.domain.model.response.AuthResponse
import com.teco.ventago.utils.ApiError
import com.teco.ventago.utils.ApiResponse
import com.teco.ventago.utils.AuthException
import com.teco.ventago.utils.BadRequestException
import com.teco.ventago.utils.BusinessChoice
import com.teco.ventago.utils.BusinessSelectionRequiredException
import com.teco.ventago.utils.isError
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

class AuthRepository(private val provider: IAuthProvider, private val logger: ILoggerService):
    IAuthRepository {
    override suspend fun googleLogin(googleToken: String, businessId: Int?): AuthResponse {
        try {
            val response = provider.googleLogin(googleToken, businessId)

            if (response.error == ApiError.AUTH_005) {
                throw BusinessSelectionRequiredException(parseBusinessChoices(response.data))
            }

            if (response.error.isError()) {
                getError(response)
            }

            if (response.data is JsonObject) {
                val map = Json.decodeFromJsonElement<Map<String, JsonElement>>(response.data)
                return AuthResponse.fromMap(map)
            } else {
                throw BadRequestException(response.toJson())
            }
        } catch (e: Exception) {
            if (e is BusinessSelectionRequiredException) throw e
            logger.sendLog(
                Log(
                    LogLevel.ERROR, "googleLogin",
                "Error making login/register. Error: ${e.message ?: "UNKNOWN" }, request: $googleToken"
                )
            )
            throw e
        }
    }

    override suspend fun emailLogin(request: EmailLoginRequest): AuthResponse {
        try {
            val response = provider.emailLogin(request)
            println("ASDASD: ${response.toJson()}")

            if (response.error == ApiError.AUTH_005) {
                throw BusinessSelectionRequiredException(parseBusinessChoices(response.data))
            }

            if (response.error.isError()) {
                getError(response)
            }

            if (response.data is JsonObject) {
                val map = Json.decodeFromJsonElement<Map<String, JsonElement>>(response.data)
                return AuthResponse.fromMap(map)
            } else {
                throw BadRequestException(response.toJson())
            }
        } catch (e: Exception) {
            if (e is BusinessSelectionRequiredException) throw e
            println("ASDASD: ${e.message}")
            logger.sendLog(
                Log(
                    LogLevel.ERROR, "emailLogin",
                    "Error making registration. Error: ${e.message ?: "UNKNOWN" }, email: ${request.email}"
                )
            )
            throw e
        }
    }

    override suspend fun emailRegister(request: CreateUserRequest): AuthResponse {
        try {
            val response = provider.emailRegister(request)

            if (response.error.isError()) {
                getError(response)
            }

            if (response.data is JsonObject) {
                val map = Json.decodeFromJsonElement<Map<String, JsonElement>>(response.data)
                return AuthResponse.fromMap(map)
            } else {
                throw BadRequestException(response.toJson())
            }
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR, "emailRegister",
                    "Error making registration. Error: ${e.message ?: "UNKNOWN" }, email: ${request.email}"
                )
            )
            throw e
        }
    }

    private fun parseBusinessChoices(data: JsonElement?): List<BusinessChoice> {
        val rows = (data as? JsonObject)?.get("businesses") as? JsonArray ?: return emptyList()
        return rows.mapNotNull { element ->
            val row = element as? JsonObject ?: return@mapNotNull null
            val id = row["id"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
            BusinessChoice(
                id = id,
                name = row["name"]?.jsonPrimitive?.contentOrNull ?: "Negocio",
                role = row["role"]?.jsonPrimitive?.contentOrNull ?: ""
            )
        }
    }

    private fun getError(response: ApiResponse) {
        when (response.error) {
            ApiError.F_AUTH_001 -> {
                throw AuthException(response.error)
            }
            ApiError.F_AUTH_002 -> {
                throw AuthException(response.error)
            }
            ApiError.F_AUTH_003 -> {
                throw AuthException(response.error)
            }
            ApiError.F_AUTH_004 -> {
                throw AuthException(response.error)
            }
            ApiError.F_AUTH_005 -> {
                throw AuthException(response.error)
            }
            ApiError.F_AUTH_006 -> {
                throw AuthException(response.error)
            }
            else -> {
                throw BadRequestException(response.toJson())
            }
        }
    }
}