package com.teco.ventago.features.orders.domain.models.requests

import com.teco.ventago.features.orders.domain.models.responses.OnsitePaymentDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PendingIntentReleaseRequest(
    @SerialName("payment_method") val paymentMethod: String,
    @SerialName("reason") val reason: String,
)

@Serializable
data class PendingIntentReleaseResponse(
    @SerialName("released") val released: Boolean = false,
    @SerialName("payment_method") val paymentMethod: String = "",
    @SerialName("next_action") val nextActionLegacy: String = "",
    @SerialName("next_actions") val nextActions: List<String> = emptyList(),
    @SerialName("order_id") val orderId: Int? = null,
    @SerialName("order_number") val orderNumber: String = "",
    @SerialName("payment_status") val paymentStatus: Int? = null,
    @SerialName("invoice_status") val invoiceStatus: Int? = null,
) {
    val nextAction: String
        get() = nextActions.firstOrNull().orEmpty().ifBlank { nextActionLegacy }

    fun allowsNextAction(action: String): Boolean {
        return nextActions.contains(action) || nextActionLegacy == action
    }
}

@Serializable
data class PendingIntentCreateRequest(
    @SerialName("payment_method") val paymentMethod: String,
    @SerialName("amount") val amount: String,
    @SerialName("expire_in_minutes") val expireInMinutes: Int,
    @SerialName("note") val note: String = "",
)

@Serializable
data class PendingIntentCreateResponse(
    @SerialName("payment_link_url") val paymentLinkUrl: String = "",
    @SerialName("url") val url: String = "",
    @SerialName("payment_status") val paymentStatus: Int = 0,
    @SerialName("invoice_status") val invoiceStatus: Int = 0,
    @SerialName("order_number") val orderNumber: String = "",
    @SerialName("payment_link") val paymentLink: PendingIntentPaymentLinkDto? = null,
    @SerialName("paymentLink") val paymentLinkCamel: PendingIntentPaymentLinkDto? = null,
    @SerialName("onsite_payment") val onsitePayment: OnsitePaymentDto? = null,
) {
    fun resolvedPaymentLinkUrl(): String {
        return paymentLink?.resolvedUrl().orEmpty()
            .ifBlank { paymentLinkCamel?.resolvedUrl().orEmpty() }
            .ifBlank { paymentLinkUrl }
            .ifBlank { url }
    }
}

@Serializable
data class PendingIntentPaymentLinkDto(
    @SerialName("payment_link_url") val paymentLinkUrl: String = "",
    @SerialName("paymentLinkUrl") val paymentLinkUrlCamel: String = "",
    @SerialName("url") val url: String = "",
    @SerialName("amount") val amount: String = "",
) {
    fun resolvedUrl(): String = paymentLinkUrl.ifBlank { paymentLinkUrlCamel }.ifBlank { url }
}
