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
import com.teco.ventago.features.pos.provisioning.domain.model.PosLinkMode
import com.teco.ventago.features.pos.provisioning.domain.model.PosProvisioningState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class PosDeviceProvisioningService(
    private val appDistribution: AppDistribution,
    private val agentConfigReader: IPosAgentConfigReader,
    private val repository: IPosDeviceProvisioningRepository,
    private val logger: ILoggerService,
    private val bindingStore: IPosDeviceBindingStore,
) {
    private val state = MutableStateFlow(initialState())
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
        val agentConfig = probeAgent()
        val fromAgent = agentConfig != null
        val binding = agentConfig ?: bindingStore.load()
        if (binding == null) {
            enterUnlinked()
            return
        }
        if (businessIds.none { it == binding.businessId }) {
            state.value = PosProvisioningState(
                required = true,
                agentConfig = binding,
                valid = false,
                linkMode = if (fromAgent) PosLinkMode.Linked else PosLinkMode.Degraded,
            )
            throw PosProvisioningException.BusinessMismatch
        }
        applyBinding(
            binding = binding,
            accessToken = accessToken,
            requireExactMatch = fromAgent,
            agentReachable = fromAgent,
        )
    }

    suspend fun refreshFromKnownDevice(): PosDeviceConfig? {
        if (!isRequired()) return null
        val agentConfig = probeAgent()
        val binding = state.value.agentConfig ?: agentConfig ?: bindingStore.load() ?: return null
        return applyBinding(
            binding = binding,
            accessToken = currentAccessToken,
            requireExactMatch = agentConfig != null,
            agentReachable = agentConfig != null,
        )
    }

    fun clear() {
        currentAccessToken = null
        PosDevicePermissionGate.clear()
        state.value = initialState()
    }

    private fun initialState(): PosProvisioningState =
        PosProvisioningState(
            required = appDistribution.isPosBuild,
            valid = !appDistribution.isPosBuild,
            linkMode = if (appDistribution.isPosBuild) PosLinkMode.Unlinked else PosLinkMode.NotRequired,
        )

    private suspend fun probeAgent(): PosAgentDeviceConfig? {
        val result = try {
            agentConfigReader.getDeviceConfig()
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR,
                    "PosDeviceProvisioningService::probeAgent",
                    "POS agent probe failed: ${e.message ?: "UNKNOWN"}"
                )
            )
            return null
        } ?: return null
        if (!result.activated) return null
        return parseAgentConfig(result.configJson)
    }

    private fun parseAgentConfig(configJson: String): PosAgentDeviceConfig? {
        val parsed = try {
            json.decodeFromString<PosAgentDeviceConfig>(configJson)
        } catch (e: SerializationException) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR,
                    "PosDeviceProvisioningService::parseAgentConfig",
                    "Invalid POS agent config JSON: ${e.message ?: "UNKNOWN"}"
                )
            )
            return null
        } catch (e: IllegalArgumentException) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR,
                    "PosDeviceProvisioningService::parseAgentConfig",
                    "Invalid POS agent config JSON: ${e.message ?: "UNKNOWN"}"
                )
            )
            return null
        }
        return parsed.takeIf { it.isComplete() }
    }

    private suspend fun applyBinding(
        binding: PosAgentDeviceConfig,
        accessToken: String?,
        requireExactMatch: Boolean,
        agentReachable: Boolean,
    ): PosDeviceConfig? {
        val deviceConfig = try {
            repository.getPosConfig(binding.deviceId, accessToken)
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR,
                    "PosDeviceProvisioningService::applyBinding",
                    "POS device config unavailable for ${binding.deviceId}: ${e.message ?: "UNKNOWN"}"
                )
            )
            enterDegraded(binding, deviceConfig = null)
            return null
        }
        if (!deviceConfig.active) {
            state.value = PosProvisioningState(
                required = true,
                agentConfig = binding,
                deviceConfig = deviceConfig,
                valid = false,
                linkMode = if (agentReachable) PosLinkMode.Linked else PosLinkMode.Degraded,
            )
            throw PosProvisioningException.DeviceInactive
        }
        if (requireExactMatch && !matchesAgent(binding, deviceConfig)) {
            state.value = PosProvisioningState(
                required = true,
                agentConfig = binding,
                deviceConfig = deviceConfig,
                valid = false,
                linkMode = PosLinkMode.Linked,
            )
            throw PosProvisioningException.DeviceConfigMismatch
        }
        val canonical = bindingFromDevice(deviceConfig)
        bindingStore.save(canonical)
        PosDevicePermissionGate.update(deviceConfig.permissions)
        state.value = PosProvisioningState(
            required = true,
            agentConfig = canonical,
            deviceConfig = deviceConfig,
            valid = true,
            linkMode = if (agentReachable) PosLinkMode.Linked else PosLinkMode.Degraded,
        )
        return deviceConfig
    }

    private fun enterDegraded(binding: PosAgentDeviceConfig, deviceConfig: PosDeviceConfig?) {
        if (deviceConfig != null) {
            PosDevicePermissionGate.update(deviceConfig.permissions)
        } else {
            PosDevicePermissionGate.clear()
        }
        state.value = PosProvisioningState(
            required = true,
            agentConfig = binding,
            deviceConfig = deviceConfig,
            valid = true,
            linkMode = PosLinkMode.Degraded,
        )
    }

    private fun enterUnlinked() {
        PosDevicePermissionGate.clear()
        state.value = PosProvisioningState(
            required = true,
            valid = true,
            linkMode = PosLinkMode.Unlinked,
        )
    }

    private fun bindingFromDevice(config: PosDeviceConfig): PosAgentDeviceConfig =
        PosAgentDeviceConfig(
            deviceId = config.deviceId,
            businessId = config.businessId,
            branchCode = config.branchCode,
            billingPointCode = config.billingPointCode,
        )

    private fun matchesAgent(agentConfig: PosAgentDeviceConfig, deviceConfig: PosDeviceConfig): Boolean =
        agentConfig.deviceId == deviceConfig.deviceId &&
            agentConfig.businessId == deviceConfig.businessId &&
            agentConfig.branchCode == deviceConfig.branchCode &&
            agentConfig.billingPointCode == deviceConfig.billingPointCode
}
