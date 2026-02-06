package com.teco.ventago.features.expenses.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExpensePayment(
    val id: Long? = null,
    @SerialName("expense_id") val expenseId: Long? = null,
    @SerialName("payment_method") val paymentMethod: String? = null,
    @SerialName("payment_status") val paymentStatus: String? = null,
    @SerialName("amount_paid") val amountPaid: Double? = null,
    val reference: String? = null,
    @SerialName("proof_file_url") val proofFileUrl: String? = null,
    @SerialName("proof_file_name") val proofFileName: String? = null,
    val notes: String? = null,
    @SerialName("payment_date") val paymentDate: String? = null,
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("is_overdue") val isOverdue: Boolean? = null,
    @SerialName("days_overdue") val daysOverdue: Int? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)
