package com.teco.ventago.features.quotes.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Quote(
    val id: Long? = null,
    @SerialName("quote_number") val quoteNumber: String? = null,
    @SerialName("display_number") val displayNumber: String? = null,
    @SerialName("branch_code") val branchCode: String? = null,
    @SerialName("customer_id") val customerId: Long? = null,
    @SerialName("customer_name") val customerName: String? = null,
    @SerialName("customer_ruc") val customerRuc: String? = null,
    @SerialName("customer_email") val customerEmail: String? = null,
    @SerialName("customer_phone") val customerPhone: String? = null,
    val status: Int? = QuoteStatus.DRAFT,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("expiry_date") val expiryDate: String? = null,
    @SerialName("quote_style") val quoteStyle: String? = null,
    @SerialName("additional_info") val additionalInfo: String? = null,
    @SerialName("include_payment_button") val includePaymentButton: Boolean? = null,
    val totals: QuoteTotals? = null,
    val lines: List<QuoteLine>? = null,
    @SerialName("final_customer_info") val finalCustomerInfo: FinalCustomerInfo? = null
) {
    val displayNumberOrQuoteNumber: String
        get() = displayNumber ?: quoteNumber ?: ""
}

@Serializable
data class FinalCustomerInfo(
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    @SerialName("id_type") val idType: String? = null,
    @SerialName("id_number") val idNumber: String? = null,
    val country: String? = null
)
