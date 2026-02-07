package com.teco.ventago.core.file

data class SharedFile(
    val bytes: ByteArray,
    val fileName: String,
    val contentType: String
)
