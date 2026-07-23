package com.teco.ventago.features.pos.provisioning.domain

import com.teco.ventago.features.pos.provisioning.domain.model.PosAgentConfigResult

interface IPosAgentConfigReader {
    suspend fun getDeviceConfig(): PosAgentConfigResult?
}
