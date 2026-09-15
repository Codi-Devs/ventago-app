package com.teco.ventago.features.pos.provisioning.domain

import com.teco.ventago.features.pos.provisioning.domain.model.PosAgentDeviceConfig

interface IPosDeviceBindingStore {
    fun load(): PosAgentDeviceConfig?
    fun save(config: PosAgentDeviceConfig)
    fun clear()
}

class InMemoryPosDeviceBindingStore(
    initial: PosAgentDeviceConfig? = null,
) : IPosDeviceBindingStore {
    private var value: PosAgentDeviceConfig? = initial

    override fun load(): PosAgentDeviceConfig? = value

    override fun save(config: PosAgentDeviceConfig) {
        value = config
    }

    override fun clear() {
        value = null
    }
}
