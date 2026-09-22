package com.teco.ventago.features.pos.provisioning.data

import com.teco.ventago.core.LocalStorage
import com.teco.ventago.features.pos.provisioning.domain.IPosDeviceBindingStore
import com.teco.ventago.features.pos.provisioning.domain.model.PosAgentDeviceConfig
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class LocalStoragePosDeviceBindingStore(
    private val storage: LocalStorage,
    private val json: Json,
) : IPosDeviceBindingStore {
    override fun load(): PosAgentDeviceConfig? {
        val raw = storage.string(KEY)?.takeIf { it.isNotBlank() } ?: return null
        return runCatching { json.decodeFromString<PosAgentDeviceConfig>(raw) }
            .getOrNull()
            ?.takeIf { it.isComplete() }
    }

    override fun save(config: PosAgentDeviceConfig) {
        if (!config.isComplete()) return
        storage.set(KEY, json.encodeToString(config))
    }

    override fun clear() {
        storage.deleteObject(KEY)
    }

    private companion object {
        const val KEY = "pos_device_binding"
    }
}
