package com.teco.ventago.features.pos.devices.domain

import com.teco.ventago.features.business.domain.BusinessService
import com.teco.ventago.features.pos.devices.data.repository.IPosDevicesRepository
import com.teco.ventago.features.pos.devices.domain.model.PosDevice
import com.teco.ventago.features.pos.devices.domain.model.PosDeviceConfig
import com.teco.ventago.features.pos.devices.domain.model.PosDevicesListRequest
import com.teco.ventago.features.pos.devices.domain.model.PosDevicesPage
import com.teco.ventago.features.pos.devices.domain.model.UpdatePosDevicePermissionsRequest
import com.teco.ventago.features.pos.devices.domain.model.UpdatePosDeviceRequest
import com.teco.ventago.features.pos.provisioning.domain.model.PosDevicePermissions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PosDevicesService(
    private val repository: IPosDevicesRepository,
    private val businessService: BusinessService,
) {
    private val devicesState = MutableStateFlow<List<PosDevice>>(emptyList())

    fun observe(): StateFlow<List<PosDevice>> = devicesState.asStateFlow()

    suspend fun listActiveDevices(page: Int = 1, pageSize: Int = 20): PosDevicesPage {
        val businessId = currentBusinessId()
        val response = repository.listDevices(
            businessId = businessId,
            request = PosDevicesListRequest(page = page, pageSize = pageSize, status = "active")
        )
        devicesState.value = response.items
        return response
    }

    suspend fun loadDeviceConfig(deviceId: String): PosDeviceConfig {
        val config = repository.getPosConfig(deviceId)
        val devices = devicesState.value.toMutableList()
        val index = devices.indexOfFirst { it.deviceId == config.deviceId }
        if (index >= 0) {
            val current = devices[index]
            devices[index] = current.copy(
                businessId = config.businessId ?: current.businessId,
                branchCode = config.branchCode,
                billingPointCode = config.billingPointCode,
                status = config.status.ifBlank { current.status },
                permissions = config.permissions,
            )
            devicesState.value = devices
        }
        return config
    }

    suspend fun updateDeviceBranchBilling(
        device: PosDevice,
        branchCode: String,
        billingPointCode: String,
    ): PosDevice {
        val businessId = currentBusinessId()
        val updated = repository.updateDevice(
            businessId = businessId,
            deviceId = device.deviceId,
            request = UpdatePosDeviceRequest(
                name = device.displayName,
                branchCode = branchCode,
                billingPointCode = billingPointCode,
            )
        )
        upsertDevice(updated)
        return updated
    }

    suspend fun updatePermissions(
        deviceId: String,
        request: UpdatePosDevicePermissionsRequest,
    ): PosDevicePermissions {
        val businessId = currentBusinessId()
        val permissions = repository.updatePermissions(businessId, deviceId, request)
        val updatedDevices = devicesState.value.map { device ->
            if (device.deviceId == deviceId) {
                device.copy(permissions = permissions)
            } else {
                device
            }
        }
        devicesState.value = updatedDevices
        return permissions
    }

    private fun upsertDevice(updated: PosDevice) {
        val devices = devicesState.value.toMutableList()
        val index = devices.indexOfFirst { it.deviceId == updated.deviceId }
        if (index >= 0) {
            val previousPermissions = devices[index].permissions
            devices[index] = updated.copy(permissions = updated.permissions ?: previousPermissions)
        } else {
            devices.add(updated)
        }
        devicesState.value = devices
    }

    private fun currentBusinessId(): Int {
        val businessId = businessService.business.value?.businessId ?: -1
        if (businessId <= 0) {
            throw IllegalStateException("No business selected")
        }
        return businessId
    }

    fun clear() {
        devicesState.value = emptyList()
    }
}
