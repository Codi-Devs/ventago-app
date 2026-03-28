package com.teco.ventago.features.orders.domain.models.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class PaymentApplicationRequest(
    @SerialName("receivable_term_id") val receivableTermId: Long,
    @SerialName("amount") val amount: String
)

@Serializable
data class ManualPaymentItemRequest(
    @SerialName("type") val type: Int,
    @SerialName("amount") val amount: String,
    @SerialName("payment_date") val paymentDate: String,
    @SerialName("description") val description: String? = null,
    @SerialName("applications") val applications: List<PaymentApplicationRequest>? = null
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

