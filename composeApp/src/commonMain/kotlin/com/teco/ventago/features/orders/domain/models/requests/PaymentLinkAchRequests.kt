package com.teco.ventago.features.orders.domain.models.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreatePaymentLinkRequest(
    @SerialName("order_id") val orderId: Int,
    @SerialName("amount") val amount: String? = null,
    @SerialName("expire_in_minutes") val expireInMinutes: Int,
)

@Serializable
data class CreatePaymentLinkResponse(
    @SerialName("payment_link_url") val paymentLinkUrl: String? = null,
)

@Serializable
data class ApproveAchPaymentResponse(
    @SerialName("status") val status: String = "",
)

@Serializable
data class RejectAchPaymentRequest(
    @SerialName("reason_code") val reasonCode: String,
    @SerialName("reason_text") val reasonText: String,
)
