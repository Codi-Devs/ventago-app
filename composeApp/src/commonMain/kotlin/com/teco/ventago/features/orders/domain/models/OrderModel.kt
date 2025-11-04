package com.teco.ventago.features.orders.domain.models

import com.teco.ventago.features.invoicing.domain.models.InvoiceStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Order(
    val id: Int,
    @SerialName("order_type") val orderType: String,
    @SerialName("business_id") val businessId: Int,
    @SerialName("internal_number") val internalNumber: String,
    @SerialName("external_invoice_number") val externalInvoiceNumber: String? = null,
    @SerialName("external_invoice_id") val externalInvoiceId: String? = null,
    @SerialName("invoice_status") val invoiceStatus: Int? = InvoiceStatus.NONE.id,
    @SerialName("currency_code") val currencyCode: String,

    val lines: List<OrderLineDto> = emptyList(),

    // Monetary totals come as strings in the payload
    val subtotal: String,
    @SerialName("discount_total") val discountTotal: String,
    @SerialName("taxable_base") val taxableBase: String,
    @SerialName("tax_total") val taxTotal: String,
    @SerialName("tips_total") val tipsTotal: String,
    @SerialName("total_amount") val totalAmount: String,

    val status: Int,
    @SerialName("payment_status") val paymentStatus: Int = PaymentStatus.UNPAID.id,

    @SerialName("order_histories") val orderHistories: List<OrderHistoryDto> = emptyList(),
    @SerialName("order_payments")  val orderPayments:  List<OrderPaymentDto> = emptyList(),

    @SerialName("payment_link") val paymentLink: String? = null,

    val customer: CustomerSnapshot? = null,

    @SerialName("created_at") val createdAt: String, // ISO8601
) {


    fun formattedInternalNumber(): String {
        return internalNumber.substringAfterLast('-')
    }
}

@Serializable
data class OrderLineDto(
    @SerialName("item_id") val itemId: Int,
    @SerialName("item_name") val itemName: String,
    @SerialName("base_unit_price") val baseUnitPrice: String,
    @SerialName("override_unit_price") val overrideUnitPrice: String? = null,
    val quantity: Int,
    @SerialName("discount_mode") val discountMode: Int,
    @SerialName("discount_value") val discountValue: String,
    @SerialName("tax_name") val taxName: String,
    @SerialName("tax_rate") val taxRate: String,
    @SerialName("line_subtotal") val lineSubtotal: String,
    @SerialName("tax_amount") val taxAmount: String,
    @SerialName("line_total") val lineTotal: String
)

@Serializable
data class OrderHistoryDto(
    @SerialName("status_id") val statusId: Int,
    val note: String,
    @SerialName("changed_by") val changedBy: String,
    @SerialName("created_at") val createdAt: String // ISO8601
)

@Serializable
data class OrderPaymentDto(
    @SerialName("payment_intent_id") val paymentIntentId: String,
    @SerialName("payment_method_id") val paymentMethod: PaymentMethodDto,
    @SerialName("payment_status_str") val paymentStatusStr: String,
    @SerialName("total_amount") val totalAmount: String,
    val charged: String,
    val refunded: String
)

@Serializable
data class PaymentMethodDto(
    @SerialName("ID") val id: Int,
    @SerialName("Name") val name: String,
    @SerialName("Description") val description: String
)

@Serializable
data class CustomerSnapshot(
    val id: Int,
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    val ruc: String? = null,
    val status: Int = 1,
    @SerialName("customer_invoice_id") val customerInvoiceID: Int? = null,
)

// --- Usage example ---

private val json = Json {
    ignoreUnknownKeys = true   // resilient to backend changes
    isLenient = true
}

fun decodeOrder(payload: String): Order =
    json.decodeFromString(Order.serializer(), payload)

fun String.moneyToCents(): Long = try {
    val clean = replace(",", ".").trim()
    val parts = clean.split('.')
    when (parts.size) {
        1 -> parts[0].toLong() * 100
        2 -> (parts[0].toLong() * 100) + parts[1].padEnd(2, '0').take(2).toLong()
        else -> 0L
    }
} catch (_: Throwable) { 0L }