package com.teco.ventago.core

import kotlinx.serialization.Serializable

@Serializable
data class Paged<T>(
    val page: Int,
    val size: Int,
    val total: Long,
    val items: List<T>
)