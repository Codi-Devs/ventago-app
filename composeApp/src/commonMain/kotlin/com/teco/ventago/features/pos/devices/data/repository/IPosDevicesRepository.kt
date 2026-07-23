package com.teco.ventago.features.pos.devices.data.repository

import com.teco.ventago.features.pos.devices.domain.model.PosDevice
import com.teco.ventago.features.pos.devices.domain.model.PosDeviceConfig
import com.teco.ventago.features.pos.devices.domain.model.PosDevicesListRequest
import com.teco.ventago.features.pos.devices.domain.model.PosDevicesPage
import com.teco.ventago.features.pos.devices.domain.model.UpdatePosDevicePermissionsRequest
import com.teco.ventago.features.pos.devices.domain.model.UpdatePosDeviceRequest
import com.teco.ventago.features.pos.provisioning.domain.model.PosDevicePermissions

interface IPosDevicesRepository {
    suspend fun listDevices(businessId: Int, request: PosDevicesListRequest): PosDevicesPage
    suspend fun getPosConfig(deviceId: String): PosDeviceConfig
    suspend fun updateDevice(businessId: Int, deviceId: String, request: UpdatePosDeviceRequest): PosDevice
    suspend fun updatePermissions(
        businessId: Int,
        deviceId: String,
        request: UpdatePosDevicePermissionsRequest,
    ): PosDevicePermissions
}
