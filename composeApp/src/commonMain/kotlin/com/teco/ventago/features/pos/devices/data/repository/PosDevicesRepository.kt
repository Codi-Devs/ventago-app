package com.teco.ventago.features.pos.devices.data.repository

import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.features.pos.devices.data.provider.IPosDevicesProvider
import com.teco.ventago.features.pos.devices.domain.model.PosDevice
import com.teco.ventago.features.pos.devices.domain.model.PosDeviceConfig
import com.teco.ventago.features.pos.devices.domain.model.PosDevicesListRequest
import com.teco.ventago.features.pos.devices.domain.model.PosDevicesPage
import com.teco.ventago.features.pos.devices.domain.model.UpdatePosDevicePermissionsRequest
import com.teco.ventago.features.pos.devices.domain.model.UpdatePosDeviceRequest
import com.teco.ventago.features.pos.provisioning.domain.model.PosDevicePermissions
import com.teco.ventago.json
import com.teco.ventago.utils.BadRequestException
import com.teco.ventago.utils.isError
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

class PosDevicesRepository(
    private val provider: IPosDevicesProvider,
    private val logger: ILoggerService,
) : IPosDevicesRepository {
    override suspend fun listDevices(businessId: Int, request: PosDevicesListRequest): PosDevicesPage {
        return runRepositoryCall("listDevices", businessId, null) {
            val response = provider.listDevices(businessId, request)
            if (!response.successful || response.error.isError() || response.data !is JsonObject) {
                throw BadRequestException(response.toJson())
            }
            json.decodeFromJsonElement(response.data)
        }
    }

    override suspend fun getPosConfig(deviceId: String): PosDeviceConfig {
        return runRepositoryCall("getPosConfig", null, deviceId) {
            val response = provider.getPosConfig(deviceId)
            if (!response.successful || response.error.isError() || response.data == null) {
                throw BadRequestException(response.toJson())
            }
            json.decodeFromJsonElement(response.data)
        }
    }

    override suspend fun updateDevice(
        businessId: Int,
        deviceId: String,
        request: UpdatePosDeviceRequest,
    ): PosDevice {
        return runRepositoryCall("updateDevice", businessId, deviceId) {
            val response = provider.updateDevice(businessId, deviceId, request)
            if (!response.successful || response.error.isError() || response.data == null) {
                throw BadRequestException(response.toJson())
            }
            json.decodeFromJsonElement(response.data)
        }
    }

    override suspend fun updatePermissions(
        businessId: Int,
        deviceId: String,
        request: UpdatePosDevicePermissionsRequest,
    ): PosDevicePermissions {
        return runRepositoryCall("updatePermissions", businessId, deviceId) {
            val response = provider.updatePermissions(businessId, deviceId, request)
            if (!response.successful || response.error.isError() || response.data == null) {
                throw BadRequestException(response.toJson())
            }
            json.decodeFromJsonElement(response.data)
        }
    }

    private suspend fun <T> runRepositoryCall(
        flow: String,
        businessId: Int?,
        deviceId: String?,
        block: suspend () -> T,
    ): T {
        return try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR,
                    "PosDevicesRepository::$flow",
                    "POS devices repository error. businessId=${businessId ?: "none"} deviceId=${deviceId ?: "none"} error=${e.message ?: "UNKNOWN"}"
                )
            )
            throw e
        }
    }
}
