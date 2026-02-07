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

@Serializable
data class UpsertExpenseRequest(
    @SerialName("business_id") val businessId: Int,
    @SerialName("invoice_number") val invoiceNumber: String? = null,
    val cufe: String? = null,
    @SerialName("emission_date") val emissionDate: String? = null,
    @SerialName("payment_method") val paymentMethod: String? = null,
    val issuer: ExpensePartyRequest,
    val receiver: ExpensePartyRequest,
    val items: List<ExpenseItemRequest>,
    val subtotal: Double,
    @SerialName("itbms_total") val itbmsTotal: Double,
    @SerialName("total_amount") val totalAmount: Double,
    val notes: String? = null,
    @SerialName("file_url") val fileUrl: String? = null,
    @SerialName("remove_file") val removeFile: Boolean? = null,
    val payment: InitialExpensePaymentRequest? = null
)

@Serializable
data class ExpensePartyRequest(
    val name: String,
    val ruc: String? = null,
    val dv: String? = null,
    val type: String? = null
)

@Serializable
data class ExpenseItemRequest(
    @SerialName("line_number") val lineNumber: Int,
    val description: String,
    val quantity: Double,
    @SerialName("unit_price") val unitPrice: Double,
    @SerialName("discount_amount") val discountAmount: Double,
    val subtotal: Double,
    @SerialName("itbms_amount") val itbmsAmount: Double,
    val total: Double
)

@Serializable
data class InitialExpensePaymentRequest(
    @SerialName("payment_method") val paymentMethod: String,
    @SerialName("amount_paid") val amountPaid: Double,
    @SerialName("payment_date") val paymentDate: String? = null,
    @SerialName("due_date") val dueDate: String? = null
)

data class ExpenseProofFile(
    val bytes: ByteArray,
    val fileName: String,
    val contentType: String = "application/octet-stream"
)
