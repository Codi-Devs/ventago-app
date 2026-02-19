package com.teco.ventago.features.orders.domain.models.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeleteOrderRequest(
    @SerialName("order_id") val orderId: Long,
    @SerialName("delete_reason") val deleteReason: String
)
