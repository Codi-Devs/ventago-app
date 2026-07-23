package com.teco.ventago.features.pos.provisioning.domain

sealed class PosProvisioningException(
    override val message: String,
) : Exception(message) {
    data object AgentUnavailable : PosProvisioningException("POS agent did not return device config.")
    data object AgentInactive : PosProvisioningException("POS device is not activated.")
    data object InvalidAgentConfig : PosProvisioningException("POS agent returned invalid device config.")
    data object BusinessMismatch : PosProvisioningException("Authenticated business does not match POS device business.")
    data object DeviceConfigUnavailable : PosProvisioningException("POS device config could not be loaded.")
    data object DeviceInactive : PosProvisioningException("POS device config is not active.")
    data object DeviceConfigMismatch : PosProvisioningException("POS device config does not match agent config.")
}
