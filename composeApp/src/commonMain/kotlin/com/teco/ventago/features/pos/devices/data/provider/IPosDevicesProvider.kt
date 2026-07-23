package com.teco.ventago.features.pos.devices.data.provider

import com.teco.ventago.features.pos.devices.domain.model.PosDevicesListRequest
import com.teco.ventago.features.pos.devices.domain.model.UpdatePosDevicePermissionsRequest
import com.teco.ventago.features.pos.devices.domain.model.UpdatePosDeviceRequest
import com.teco.ventago.utils.ApiResponse

interface IPosDevicesProvider {
    suspend fun listDevices(businessId: Int, request: PosDevicesListRequest): ApiResponse
    suspend fun getPosConfig(deviceId: String): ApiResponse

    suspend fun updateDevice(
        businessId: Int,
        deviceId: String,
        request: UpdatePosDeviceRequest,
    ): ApiResponse

    suspend fun updatePermissions(
        businessId: Int,
        deviceId: String,
        request: UpdatePosDevicePermissionsRequest,
    ): ApiResponse
}
