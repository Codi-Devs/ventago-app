package com.teco.ventago.features.pos.provisioning.domain

import com.teco.ventago.AppDistribution
import com.teco.ventago.core.authz.PosDevicePermissionGate
import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.features.auth.domain.model.response.AuthResponse
import com.teco.ventago.features.pos.provisioning.data.repository.IPosDeviceProvisioningRepository
import com.teco.ventago.features.pos.provisioning.domain.model.PosAgentDeviceConfig
import com.teco.ventago.features.pos.provisioning.domain.model.PosDeviceConfig
import com.teco.ventago.features.pos.provisioning.domain.model.PosProvisioningState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class PosDeviceProvisioningService(
    private val appDistribution: AppDistribution,
    private val agentConfigReader: IPosAgentConfigReader,
    private val repository: IPosDeviceProvisioningRepository,
    private val logger: ILoggerService,
) {
    private val state = MutableStateFlow(PosProvisioningState(required = appDistribution.isPosBuild))
    private var currentAccessToken: String? = null
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun observe(): StateFlow<PosProvisioningState> = state.asStateFlow()

    fun currentState(): PosProvisioningState = state.value

    fun isRequired(): Boolean = appDistribution.isPosBuild

    suspend fun validateLogin(response: AuthResponse) {
        if (!isRequired()) return
        validateBusinessIds(response.businesses.map { it.businessId }, response.accessToken)
    }

    suspend fun validateBusinessIds(businessIds: List<Int>, accessToken: String? = currentAccessToken) {
        if (!isRequired()) return
        currentAccessToken = accessToken
        val agentConfig = loadAgentConfig()
        val userBusinessMatches = businessIds.any { it == agentConfig.businessId }
        if (!userBusinessMatches) {
            state.value = PosProvisioningState(required = true, agentConfig = agentConfig, valid = false)
            throw PosProvisioningException.BusinessMismatch
        }
        refreshBackendConfig(agentConfig, accessToken)
    }

    suspend fun refreshFromKnownDevice(): PosDeviceConfig? {
        if (!isRequired()) return null
        val agentConfig = state.value.agentConfig ?: loadAgentConfig()
        return refreshBackendConfig(agentConfig, currentAccessToken)
    }

    fun clear() {
        currentAccessToken = null
        PosDevicePermissionGate.clear()
        state.value = PosProvisioningState(
            required = appDistribution.isPosBuild,
            valid = !appDistribution.isPosBuild
        )
    }

    private suspend fun loadAgentConfig(): PosAgentDeviceConfig {
        val result = agentConfigReader.getDeviceConfig()
            ?: throw PosProvisioningException.AgentUnavailable
        if (!result.activated) {
            throw PosProvisioningException.AgentInactive
        }
        val parsed = try {
            json.decodeFromString<PosAgentDeviceConfig>(result.configJson)
        } catch (e: SerializationException) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR,
                    "PosDeviceProvisioningService::loadAgentConfig",
                    "Invalid POS agent config JSON: ${e.message ?: "UNKNOWN"}"
                )
            )
            throw PosProvisioningException.InvalidAgentConfig
        } catch (e: IllegalArgumentException) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR,
                    "PosDeviceProvisioningService::loadAgentConfig",
                    "Invalid POS agent config JSON: ${e.message ?: "UNKNOWN"}"
                )
            )
            throw PosProvisioningException.InvalidAgentConfig
        }
        if (!parsed.isComplete()) {
            throw PosProvisioningException.InvalidAgentConfig
        }
        return parsed
    }

    private suspend fun refreshBackendConfig(
        agentConfig: PosAgentDeviceConfig,
        accessToken: String?
    ): PosDeviceConfig {
        val deviceConfig = try {
            repository.getPosConfig(agentConfig.deviceId, accessToken)
        } catch (_: Exception) {
            throw PosProvisioningException.DeviceConfigUnavailable
        }
        if (!deviceConfig.active) {
            state.value = PosProvisioningState(
                required = true,
                agentConfig = agentConfig,
                deviceConfig = deviceConfig,
                valid = false
            )
            throw PosProvisioningException.DeviceInactive
        }
        if (!matchesAgent(agentConfig, deviceConfig)) {
            state.value = PosProvisioningState(
                required = true,
                agentConfig = agentConfig,
                deviceConfig = deviceConfig,
                valid = false
            )
            throw PosProvisioningException.DeviceConfigMismatch
        }
        PosDevicePermissionGate.update(deviceConfig.permissions)
        state.value = PosProvisioningState(
            required = true,
            agentConfig = agentConfig,
            deviceConfig = deviceConfig,
            valid = true
        )
        return deviceConfig
    }

    private fun matchesAgent(agentConfig: PosAgentDeviceConfig, deviceConfig: PosDeviceConfig): Boolean =
        agentConfig.deviceId == deviceConfig.deviceId &&
            agentConfig.businessId == deviceConfig.businessId &&
            agentConfig.branchCode == deviceConfig.branchCode &&
            agentConfig.billingPointCode == deviceConfig.billingPointCode
}
