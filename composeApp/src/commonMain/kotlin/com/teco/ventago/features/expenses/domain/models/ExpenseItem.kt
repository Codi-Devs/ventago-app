package com.teco.ventago.features.expenses.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExpenseItem(
    val id: Long? = null,
    @SerialName("line_number") val lineNumber: Int? = null,
    @SerialName("item_code") val itemCode: String? = null,
    val description: String? = null,
    val quantity: Double? = null,
    @SerialName("unit_price") val unitPrice: Double? = null,
    @SerialName("discount_amount") val discountAmount: Double? = null,
    val subtotal: Double? = null,
    @SerialName("itbms_amount") val itbmsAmount: Double? = null,
    val total: Double? = null,
    @SerialName("expense_account_id") val expenseAccountId: Long? = null,
    @SerialName("expense_account") val expenseAccount: ExpenseAccount? = null
)
