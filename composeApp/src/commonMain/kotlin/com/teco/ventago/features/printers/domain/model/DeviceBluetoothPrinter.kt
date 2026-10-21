package com.teco.ventago.features.printers.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class DeviceBluetoothPrinter(
    val id: String,
    val name: String,
    val paperWidthMm: Int = 58,
    val isActive: Boolean = true,
)
