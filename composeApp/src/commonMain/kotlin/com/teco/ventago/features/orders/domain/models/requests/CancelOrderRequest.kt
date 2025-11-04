package com.teco.ventago.features.orders.domain.models.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CancelOrderRequest(
    @SerialName("order_id") val orderId: Long,
    val reason: String,
    @SerialName("user_name") val userName: String
)