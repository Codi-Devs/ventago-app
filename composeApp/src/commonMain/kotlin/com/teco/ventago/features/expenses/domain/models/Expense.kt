package com.teco.ventago.features.expenses.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Expense(
    val id: Long? = null,
    @SerialName("business_id") val businessId: Int? = null,
    @SerialName("invoice_number") val invoiceNumber: String? = null,
    val cufe: String? = null,
    @SerialName("emission_date") val emissionDate: String? = null,
    val issuer: ExpenseParty? = null,
    val receiver: ExpenseParty? = null,
    val subtotal: Double? = null,
    @SerialName("itbms_total") val itbmsTotal: Double? = null,
    @SerialName("total_amount") val totalAmount: Double? = null,
    @SerialName("currency_code") val currencyCode: String? = null,
    @SerialName("payment_method") val paymentMethod: String? = null,
    val notes: String? = null,
    @SerialName("authorization_protocol") val authorizationProtocol: String? = null,
    @SerialName("authorization_date") val authorizationDate: String? = null,
    @SerialName("file_url") val fileUrl: String? = null,
    val source: String? = null,
    @SerialName("default_account_id") val defaultAccountId: Long? = null,
    @SerialName("default_account") val defaultAccount: ExpenseAccount? = null,
    @SerialName("categorization_status") val categorizationStatus: String? = null,
    @SerialName("categorized_items_count") val categorizedItemsCount: Int? = null,
    @SerialName("total_items_count") val totalItemsCount: Int? = null,
    val items: List<ExpenseItem>? = null,
    val payments: List<ExpensePayment>? = null,
    @SerialName("payment_status") val paymentStatus: String? = null,
    @SerialName("payment_summary") val paymentSummary: PaymentSummary? = null,
    @SerialName("total_paid") val totalPaid: Double? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    val isManual: Boolean get() = source == "manual"
}

@Serializable
data class PaymentSummary(
    @SerialName("total_paid") val totalPaid: Double? = null,
    val remaining: Double? = null,
    val status: String? = null
)
