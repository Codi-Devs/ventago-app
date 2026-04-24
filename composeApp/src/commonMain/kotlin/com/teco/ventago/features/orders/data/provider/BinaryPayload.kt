package com.teco.ventago.features.orders.data.provider

data class BinaryPayload(
    val bytes: ByteArray,
    val contentType: String? = null,
    val fileName: String? = null,
)
