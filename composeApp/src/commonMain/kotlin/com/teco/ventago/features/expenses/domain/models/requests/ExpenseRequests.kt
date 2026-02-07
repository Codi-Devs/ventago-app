package com.teco.ventago.features.expenses.domain.models.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ListExpensesRequest(
    @SerialName("business_id") val businessId: Int,
    val page: Int = 1,
    @SerialName("page_size") val pageSize: Int = 10,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
    val source: String? = null,
    @SerialName("issuer_name") val issuerName: String? = null,
    @SerialName("issuer_ruc") val issuerRuc: String? = null,
    @SerialName("invoice_number") val invoiceNumber: String? = null
)

@Serializable
data class UpsertExpensePaymentRequest(
    @SerialName("payment_method") val paymentMethod: String,
    @SerialName("amount_paid") val amountPaid: Double,
    val reference: String? = null,
    val notes: String? = null,
    @SerialName("payment_date") val paymentDate: String? = null,
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("proof_file_url") val proofFileUrl: String? = null,
    @SerialName("payment_status") val paymentStatus: String? = null
)

data class ExpenseProofFile(
    val bytes: ByteArray,
    val fileName: String,
    val contentType: String = "application/octet-stream"
)
