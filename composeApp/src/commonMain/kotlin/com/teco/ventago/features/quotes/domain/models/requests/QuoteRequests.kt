package com.teco.ventago.features.quotes.domain.models.requests

import com.teco.ventago.features.orders.domain.models.requests.InvoiceCharge
import com.teco.ventago.features.orders.domain.models.requests.InvoiceDiscount
import com.teco.ventago.features.orders.domain.models.requests.NameValue
import com.teco.ventago.features.orders.domain.models.requests.OrderItem
import com.teco.ventago.features.orders.domain.models.requests.OrderItemDiscount
import com.teco.ventago.features.orders.domain.models.requests.OrderItemTax
import com.teco.ventago.features.orders.domain.models.requests.Charge
import com.teco.ventago.features.orders.domain.models.requests.ItemTotals
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ListQuotesRequest(
    val page: Int = 0,
    @SerialName("page_size") val pageSize: Int = 10,
    @SerialName("customer_name") val customerName: String? = null,
    @SerialName("customer_ruc") val customerRuc: String? = null,
    @SerialName("quote_number") val quoteNumber: String? = null,
    val status: Int? = null
)

@Serializable
data class GetQuoteRequest(
    @SerialName("quote_id") val quoteId: Long? = null,
    @SerialName("quote_number") val quoteNumber: String? = null
)

@Serializable
data class SendQuoteEmailRequest(
    @SerialName("quote_id") val quoteId: Long,
    @SerialName("recipient_email") val recipientEmail: String
)

@Serializable
data class CancelQuoteRequest(
    @SerialName("quote_id") val quoteId: Long,
    val reason: String
)

@Serializable
data class QuoteTotalsRequest(
    @SerialName("quantity_items") val quantityItems: Int,
    val charges: List<InvoiceCharge> = emptyList(),
    val discounts: List<InvoiceDiscount> = emptyList(),
    val subtotal: String,
    @SerialName("total_before_discounts") val totalBeforeDiscounts: String,
    @SerialName("total_after_discounts") val totalAfterDiscounts: String,
    @SerialName("total_before_taxes") val totalBeforeTaxes: String,
    @SerialName("total_after_taxes") val totalAfterTaxes: String,
    @SerialName("total_taxes") val totalTaxes: String,
    @SerialName("invoice_total") val invoiceTotal: String
)

@Serializable
data class QuoteFinalCustomerInfo(
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    @SerialName("id_type") val idType: String? = null,
    @SerialName("id_number") val idNumber: String? = null,
    val country: String? = null
)

@Serializable
data class CreateQuoteRequest(
    @SerialName("branch_code") val branchCode: String? = null,
    @SerialName("customer_id") val customerId: Long? = null,
    @SerialName("final_customer") val finalCustomer: Boolean = false,
    @SerialName("final_customer_info") val finalCustomerInfo: QuoteFinalCustomerInfo? = null,
    @SerialName("quote_style") val quoteStyle: String = "style1",
    @SerialName("expiry_date") val expiryDate: String? = null,
    val items: List<OrderItem> = emptyList(),
    val totals: QuoteTotalsRequest,
    @SerialName("include_payment_button") val includePaymentButton: Boolean = false,
    @SerialName("additional_info") val additionalInfo: String? = null,
    @SerialName("quote_id") val quoteId: Long? = null
)

@Serializable
data class UpdateQuoteRequest(
    @SerialName("customer_id") val customerId: Long? = null,
    @SerialName("final_customer") val finalCustomer: Boolean = false,
    @SerialName("final_customer_info") val finalCustomerInfo: QuoteFinalCustomerInfo? = null,
    @SerialName("quote_style") val quoteStyle: String = "style1",
    @SerialName("expiry_date") val expiryDate: String? = null,
    val items: List<OrderItem> = emptyList(),
    val totals: QuoteTotalsRequest,
    @SerialName("include_payment_button") val includePaymentButton: Boolean = false,
    @SerialName("additional_info") val additionalInfo: String? = null,
    @SerialName("quote_id") val quoteId: Long
)

/**
 * Helper alias to avoid re-importing when building items.
 */
typealias QuoteOrderItem = OrderItem
typealias QuoteOrderItemTax = OrderItemTax
typealias QuoteOrderItemDiscount = OrderItemDiscount
typealias QuoteItemTotals = ItemTotals
typealias QuoteAdditionalCharge = Charge
