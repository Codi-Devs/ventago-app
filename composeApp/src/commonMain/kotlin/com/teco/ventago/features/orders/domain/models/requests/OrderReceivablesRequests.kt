package com.teco.ventago.features.orders.domain.models.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FindOrderByIdRequest(
    @SerialName("order_id") val orderId: Int
)

@Serializable
data class RescheduleReceivableTermRequest(
    @SerialName("due_date") val dueDate: String,
    @SerialName("amount") val amount: String,
    @SerialName("notes") val notes: String = ""
)

@Serializable
data class RescheduleReceivablesRequest(
    @SerialName("source_term_ids") val sourceTermIds: List<Long> = emptyList(),
    @SerialName("new_terms") val newTerms: List<RescheduleReceivableTermRequest>,
    @SerialName("note") val note: String = ""
)

@Serializable
data class RescheduleReceivablesResponse(
    @SerialName("order_id") val orderId: Long,
    @SerialName("payment_status") val paymentStatus: Int
)

@Serializable
data class VoidOrderPaymentRequest(
    @SerialName("reason") val reason: String
)

@Serializable
data class VoidOrderPaymentResponse(
    @SerialName("order_id") val orderId: Long,
    @SerialName("payment_id") val paymentId: Long,
    @SerialName("payment_status") val paymentStatus: Int
)
