package com.teco.ventago.features.pos.provisioning.data.repository

import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.features.pos.provisioning.data.provider.IPosDeviceProvisioningProvider
import com.teco.ventago.features.pos.provisioning.domain.model.PosDeviceConfig
import com.teco.ventago.utils.BadRequestException
import com.teco.ventago.utils.isError
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

interface IPosDeviceProvisioningRepository {
    suspend fun getPosConfig(deviceId: String, accessToken: String?): PosDeviceConfig
}

class PosDeviceProvisioningRepository(
    private val provider: IPosDeviceProvisioningProvider,
    private val logger: ILoggerService,
) : IPosDeviceProvisioningRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun getPosConfig(deviceId: String, accessToken: String?): PosDeviceConfig {
        try {
            val response = provider.getPosConfig(deviceId, accessToken)
            if (!response.successful || response.error.isError() || response.data !is JsonObject) {
                throw BadRequestException(response.toJson())
            }
            return json.decodeFromJsonElement(response.data)
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR,
                    "PosDeviceProvisioningRepository::getPosConfig",
                    "Error getting POS config for deviceId=$deviceId. Error: ${e.message ?: "UNKNOWN"}"
                )
            )
            throw e
        }
    }
}
