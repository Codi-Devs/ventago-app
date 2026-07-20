package com.teco.ventago.features.orders.domain.models

import com.teco.ventago.features.invoicing.domain.models.InvoiceStatus
import com.teco.ventago.features.invoicing.domain.models.FeCustomerType
import com.teco.ventago.utils.toLongCents
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

@OptIn(ExperimentalSerializationApi::class)
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
    @SerialName("acarreo_total") val acarreoTotal: String? = null,
    @SerialName("insurance_total") val insuranceTotal: String? = null,
    @SerialName("other_charges_total") val otherChargesTotal: String? = null,
    @SerialName("total_amount") val totalAmount: String,

    val status: Int,
    @SerialName("payment_status") val paymentStatus: Int = PaymentStatus.UNPAID.id,
    @SerialName("receiver_name") val receiverName: String? = null,
    @SerialName("receiver_phone") val receiverPhone: String? = null,

    @SerialName("order_histories") val orderHistories: List<OrderHistoryDto> = emptyList(),
    @SerialName("order_payments")  val orderPayments:  List<OrderPaymentDto> = emptyList(),
    @SerialName("receivable_terms") val receivableTerms: List<ReceivableTermDto> = emptyList(),
    @SerialName("related_documents") val relatedDocuments: List<RelatedDocument> = emptyList(),

    @JsonNames("paymentLink") @SerialName("payment_link") val paymentLink: String? = null,
    @JsonNames("paymentLinks") @SerialName("payment_links") val paymentLinks: List<OrderPaymentLinkDto> = emptyList(),
    @SerialName("links") val links: List<OrderPaymentLinkDto> = emptyList(),
    @JsonNames("invoiceWarningCode") @SerialName("invoice_warning_code") val invoiceWarningCode: String? = null,
    @JsonNames("invoiceWarningMessage") @SerialName("invoice_warning_message") val invoiceWarningMessage: String? = null,
    @SerialName("external_uuid") val externalUuid: String? = null,
    @SerialName("payment_flow_type") val paymentFlowType: String? = null,
    @SerialName("ticket_enabled") val ticketEnabled: Boolean? = null,

    val customer: CustomerSnapshot? = null,

    @SerialName("created_at") val createdAt: String, // ISO8601
) {


    fun formattedInternalNumber(): String {
        return internalNumber.substringAfterLast('-')
    }

    fun displayCustomerName(): String? {
        val customerName = customer?.name?.trim().takeUnless { it.isNullOrEmpty() }
        val normalizedReceiverName = receiverName?.trim().takeUnless { it.isNullOrEmpty() }
        val shouldUseReceiverName = normalizedReceiverName != null &&
            (customer?.feCustomerType == FeCustomerType.FINAL_CONSUMER.code || customer == null)

        return when {
            shouldUseReceiverName -> normalizedReceiverName
            customerName != null -> customerName
            else -> normalizedReceiverName
        }
    }

    fun displayCustomerPhone(): String? {
        val customerPhone = customer?.phone?.trim().takeUnless {
            it.isNullOrEmpty() || it == customer?.id?.toString() || it == "0000"
        }
        val normalizedReceiverPhone = receiverPhone?.trim().takeUnless {
            it.isNullOrEmpty() || it == "0000"
        }
        return customerPhone ?: normalizedReceiverPhone
    }

    fun displayCustomerSnapshot(): CustomerSnapshot? {
        val displayName = displayCustomerName() ?: return customer
        val displayPhone = displayCustomerPhone()

        return when {
            customer == null -> CustomerSnapshot(
                id = 0,
                name = displayName,
                phone = displayPhone,
                feCustomerType = if (receiverName.isNullOrBlank()) null else FeCustomerType.FINAL_CONSUMER.code,
            )

            displayName == customer.name && displayPhone == customer.phone -> customer

            else -> customer.copy(
                name = displayName,
                phone = displayPhone ?: customer.phone,
            )
        }
    }

    fun activeCreditNoteTotalCents(): Long {
        return relatedDocuments
            .filter { it.isCreditNote() && it.isActiveForCreditLimit() }
            .sumOf { it.totalAmount.toLongCents() }
    }

    fun remainingCreditNoteCapacityCents(): Long {
        return (totalAmount.toLongCents() - activeCreditNoteTotalCents()).coerceAtLeast(0L)
    }

    fun isCreditNoteDocument(): Boolean {
        return orderType in CREDIT_NOTE_DOCUMENT_TYPES
    }

    fun supportsReceivableActions(): Boolean {
        return !isCreditNoteDocument()
    }
}

@Serializable
data class RelatedDocument(
    @SerialName("order_id") val orderId: Long,
    @SerialName("order_number") val orderNumber: String,
    @SerialName("document_type") val documentType: String,
    @SerialName("relation_type") val relationType: String,
    @SerialName("total_amount") val totalAmount: String,
    @SerialName("invoice_status") val invoiceStatus: Int,
    @SerialName("external_invoice_number") val externalInvoiceNumber: String? = null,
    @SerialName("emission_date") val emissionDate: String? = null,
) {
    fun isCreditNote(): Boolean {
        return relationType == RELATED_DOCUMENT_CREDIT_NOTE ||
            documentType in CREDIT_NOTE_DOCUMENT_TYPES
    }

    fun isDebitNote(): Boolean {
        return relationType == RELATED_DOCUMENT_DEBIT_NOTE ||
            documentType in DEBIT_NOTE_DOCUMENT_TYPES
    }

    fun isActiveForCreditLimit(): Boolean {
        return invoiceStatus == InvoiceStatus.PENDING.id ||
            invoiceStatus == InvoiceStatus.ISSUED.id
    }

    fun displayType(): String {
        return when {
            isCreditNote() -> "Nota de crédito"
            isDebitNote() -> "Nota de débito"
            else -> "Documento"
        }
    }

    fun displayOrderType(): String {
        return if (documentType.isBlank()) {
            displayType()
        } else {
            "$documentType - ${displayType()}"
        }
    }
}

private const val RELATED_DOCUMENT_CREDIT_NOTE = "credit_note"
private const val RELATED_DOCUMENT_DEBIT_NOTE = "debit_note"
private val CREDIT_NOTE_DOCUMENT_TYPES = setOf("04", "06")
private val DEBIT_NOTE_DOCUMENT_TYPES = setOf("05", "07")

@Serializable
data class ReceivableTermDto(
    val id: Long,
    @SerialName("source_order_term_id") val sourceOrderTermId: Long? = null,
    @SerialName("term_number") val termNumber: Int,
    @SerialName("due_date") val dueDateUnixSeconds: Long,
    @SerialName("original_amount") val originalAmount: String,
    @SerialName("open_amount") val openAmount: String,
    val status: Int = PaymentStatus.UNPAID.id,
    @SerialName("term_kind") val termKind: String,
    val notes: String? = null
)

@Serializable
data class OrderLineDto(
    @SerialName("item_id") val itemId: Int,
    @SerialName("item_name") val itemName: String,
    @SerialName("base_unit_price") val baseUnitPrice: String,
    @SerialName("override_unit_price") val overrideUnitPrice: String? = null,
    @Serializable(with = FlexibleDoubleSerializer::class)
    val quantity: Double,
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
    @SerialName("status_id") val statusId: Int = 0,
    val note: String = "",
    @SerialName("changed_by") val changedBy: String = "",
    @SerialName("created_at") val createdAt: String = "" // ISO8601
)

@Serializable
data class OrderPaymentLinkDto(
    @JsonNames("payment_link_url") @SerialName("link") val link: String? = null,
    @SerialName("url") val url: String? = null,
    val status: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
)

@Serializable
data class OrderPaymentDto(
    val id: Long? = null,
    @SerialName("payment_intent_id") val paymentIntentId: String = "",
    @SerialName("payment_method_id") val paymentMethod: PaymentMethodDto = PaymentMethodDto(
        id = 0,
        name = "N/A",
        description = ""
    ),
    @SerialName("payment_status_str") val paymentStatusStr: String = "",
    @SerialName("payment_date") val paymentDate: String? = null,
    @SerialName("voided_at") val voidedAt: String? = null,
    @SerialName("void_reason") val voidReason: String? = null,
    @SerialName("total_amount") val totalAmount: String = "0.00",
    val charged: String = "0.00",
    val refunded: String = "0.00",
    @SerialName("is_automatic") val isAutomatic: Boolean = false,
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
    @SerialName("fe_customer_type") val feCustomerType: String? = null,
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

object FlexibleDoubleSerializer : KSerializer<Double> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleDouble", PrimitiveKind.DOUBLE)

    override fun serialize(encoder: Encoder, value: Double) {
        encoder.encodeDouble(value)
    }

    override fun deserialize(decoder: Decoder): Double {
        if (decoder !is JsonDecoder) return decoder.decodeDouble()

        val element = decoder.decodeJsonElement()
        if (element is JsonNull) return 0.0
        val primitive = element as? JsonPrimitive ?: return 0.0

        primitive.doubleOrNull?.let { return it }
        primitive.longOrNull?.let { return it.toDouble() }
        primitive.intOrNull?.let { return it.toDouble() }

        return primitive.content
            .trim()
            .replace(',', '.')
            .toDoubleOrNull()
            ?: 0.0
    }
}
