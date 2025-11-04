package com.teco.ventago.features.orders.domain.models.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class ManualPaymentItemRequest(
    @SerialName("type") val type: Int,              // payment method code
    @SerialName("amount") val amount: String ,       // decimal string, e.g. "3.00"
    @SerialName("description") val description: String? = null        // decimal string, e.g. "3.00"
)

@Serializable
data class RegisterManualPaymentsRequest(
    @SerialName("payments") val payments: List<ManualPaymentItemRequest>
)

@Serializable
data class RegisterManualPaymentsDataResponse(
    @SerialName("order_id") val orderId: Long,
    @SerialName("order_number") val orderNumber: String,
    @SerialName("payment_status") val paymentStatus: Int,
    @SerialName("invoice_status") val invoiceStatus: Int,
    @SerialName("invoiced") val invoiced: Boolean
)


