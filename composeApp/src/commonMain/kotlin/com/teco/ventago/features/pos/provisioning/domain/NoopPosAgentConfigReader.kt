package com.teco.ventago.features.pos.provisioning.domain

import com.teco.ventago.features.pos.provisioning.domain.model.PosAgentConfigResult

class NoopPosAgentConfigReader : IPosAgentConfigReader {
    override suspend fun getDeviceConfig(): PosAgentConfigResult? = null
}
