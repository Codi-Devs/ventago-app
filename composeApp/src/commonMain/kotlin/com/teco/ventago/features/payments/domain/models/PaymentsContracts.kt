@file:OptIn(ExperimentalSerializationApi::class)

package com.teco.ventago.features.payments.domain.models

import com.teco.ventago.features.financialProfile.domain.model.MoneyAmountToCentsSerializer
import com.teco.ventago.features.printers.domain.model.TicketDocumentPayload
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNames

@Serializable
data class AchAccount(
    @SerialName("bank_code") val bankCode: String = "",
    @SerialName("bank_name") val bankName: String = "",
    @SerialName("account_type") val accountType: String = "",
    @SerialName("account_number") val accountNumber: String = "",
    @SerialName("account_number_masked") val accountNumberMasked: String = "",
    @SerialName("account_holder_name") val accountHolderName: String = "",
)

@Serializable
data class AchStatus(
    @SerialName("configured") val configured: Boolean = false,
    @SerialName("enabled") val enabled: Boolean = false,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("pending_review_count") val pendingReviewCount: Int = 0,
    @SerialName("account") val account: AchAccount? = null,
)

@Serializable
data class AchAccountConfigRequest(
    @SerialName("bank_code") val bankCode: String,
    @SerialName("bank_name") val bankName: String,
    @SerialName("account_type") val accountType: String,
    @SerialName("account_number") val accountNumber: String? = null,
    @SerialName("account_holder_name") val accountHolderName: String,
    @SerialName("account_holder_document") val accountHolderDocument: String = "",
    @SerialName("currency_code") val currencyCode: String = "USD",
    @SerialName("instructions_text") val instructionsText: String,
    @SerialName("amount_tolerance") val amountTolerance: String = "0.02",
    @SerialName("reference_required") val referenceRequired: Boolean = true,
    @SerialName("payment_validity_minutes") val paymentValidityMinutes: Int = 10080,
    @SerialName("is_active") val isActive: Boolean = true,
)

@Serializable
data class FeeSummary(
    @SerialName("currency_code") val currencyCode: String = "USD",
    @Serializable(with = MoneyAmountToCentsSerializer::class)
    @SerialName("pending_due_amount") val pendingDueAmount: Long = 0L,
    @Serializable(with = MoneyAmountToCentsSerializer::class)
    @SerialName("overdue_amount") val overdueAmount: Long = 0L,
    @Serializable(with = MoneyAmountToCentsSerializer::class)
    @SerialName("accrued_current_period_amount") val accruedCurrentPeriodAmount: Long = 0L,
    @Serializable(with = MoneyAmountToCentsSerializer::class)
    @SerialName("paid_amount") val paidAmount: Long = 0L,
    @SerialName("next_batch_generation_at") val nextBatchGenerationAt: String = "",
    @SerialName("next_due_at") val nextDueAt: String = "",
)

@Serializable
data class FeeTransactionItem(
    @SerialName("id") val id: Long? = null,
    @JsonNames("related_order_number")
    @SerialName("order_number") val relatedOrderNumber: String = "",
    @SerialName("order_id") val orderId: Long? = null,
    @SerialName("payment_method") val paymentMethod: String = "",
    @Serializable(with = MoneyAmountToCentsSerializer::class)
    @JsonNames("fee_generated")
    @SerialName("fee_amount") val feeGenerated: Long = 0L,
    @SerialName("currency_code") val currencyCode: String = "USD",
    @JsonNames("date")
    @SerialName("created_at") val date: String = "",
    @JsonNames("fee_status")
    @SerialName("status") val feeStatus: String = "",
    @JsonNames("billing_bucket")
    @SerialName("bucket") val billingBucket: String = "",
    @SerialName("batch_id") val batchId: Long? = null,
    @SerialName("batch_status") val batchStatus: String? = null,
)

@Serializable
data class FeeBatchItem(
    @SerialName("id") val id: Long? = null,
    @SerialName("period_start") val periodStart: String = "",
    @SerialName("period_end") val periodEnd: String = "",
    @SerialName("status") val status: String = "",
    @SerialName("total_lines") val totalLines: Int = 0,
    @Serializable(with = MoneyAmountToCentsSerializer::class)
    @SerialName("platform_fee_total") val platformFeeTotal: Long = 0L,
    @Serializable(with = MoneyAmountToCentsSerializer::class)
    @SerialName("tenant_markup_total") val tenantMarkupTotal: Long = 0L,
    @Serializable(with = MoneyAmountToCentsSerializer::class)
    @JsonNames("total_fee_total")
    @SerialName("total_amount") val totalFeeTotal: Long = 0L,
    @SerialName("currency_code") val currencyCode: String = "USD",
    @SerialName("issued_at") val issuedAt: String? = null,
    @SerialName("due_at") val dueAt: String? = null,
    @SerialName("paid_at") val paidAt: String? = null,
)

@Serializable
data class PaginationMeta(
    @SerialName("total") val total: Int = 0,
)

@Serializable
data class FeeTransactionsPayload(
    @SerialName("items") val items: List<FeeTransactionItem> = emptyList(),
    @SerialName("pagination") val pagination: PaginationMeta = PaginationMeta(),
    @SerialName("total") val total: Int = 0,
    @SerialName("page") val page: Int = 1,
    @SerialName("size") val size: Int = 20,
)

@Serializable
data class FeeBatchesPayload(
    @SerialName("items") val items: List<FeeBatchItem> = emptyList(),
    @SerialName("pagination") val pagination: PaginationMeta = PaginationMeta(),
    @SerialName("total") val total: Int = 0,
    @SerialName("page") val page: Int = 1,
    @SerialName("size") val size: Int = 20,
)

@Serializable
data class DirectCheckoutRequest(
    @SerialName("product_type") val productType: String = "fee_batch_payment",
    @SerialName("success_url") val successUrl: String,
    @SerialName("cancel_url") val cancelUrl: String,
)

@Serializable
data class DirectCheckoutResponse(
    @SerialName("intent_id") val intentId: Long? = null,
    @SerialName("product_type") val productType: String = "",
    @SerialName("business_id") val businessId: Int? = null,
    @SerialName("currency_code") val currencyCode: String = "USD",
    @Serializable(with = MoneyAmountToCentsSerializer::class)
    @SerialName("base_amount") val baseAmount: Long = 0L,
    @Serializable(with = MoneyAmountToCentsSerializer::class)
    @SerialName("tax_amount") val taxAmount: Long = 0L,
    @Serializable(with = MoneyAmountToCentsSerializer::class)
    @SerialName("total_amount") val totalAmount: Long = 0L,
    @JsonNames("url")
    @SerialName("payment_link_url") val paymentLinkUrl: String = "",
    @SerialName("payment_link_external_uuid") val paymentLinkExternalUuid: String = "",
    @SerialName("purchase_reference") val purchaseReference: String = "",
    @SerialName("order_number") val orderNumber: String = "",
    @SerialName("payment_status") val paymentStatus: String = "",
    @SerialName("fulfillment_status") val fulfillmentStatus: String = "",
    @SerialName("invoice_status") val invoiceStatus: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
)

@Serializable
data class TiloPayStatus(
    @SerialName("configured") val configured: Boolean = false,
    @SerialName("enabled") val enabled: Boolean = false,
    @SerialName("platform_allowed") val platformAllowed: Boolean = false,
    @SerialName("business_enabled") val businessEnabled: Boolean = false,
    @SerialName("provider") val provider: String = "tilopay",
) {
    fun readyForPayments(): Boolean = configured && enabled && platformAllowed && businessEnabled
}

@Serializable
data class TiloPayCredentialsRequest(
    @SerialName("api_user") val apiUser: String,
    @SerialName("password") val password: String,
    @SerialName("api_key") val apiKey: String,
)

@Serializable
data class YappyOnsiteGroupConfigRequest(
    @SerialName("name") val name: String,
    @SerialName("api_key") val apiKey: String? = null,
    @SerialName("secret_key") val secretKey: String? = null,
    @SerialName("branch_code") val branchCode: String,
)

@Serializable
data class YappyOnsiteGroup(
    @Serializable(with = FlexibleStringSerializer::class)
    @SerialName("id") val id: String? = null,
    @SerialName("business_id") val businessId: Int? = null,
    @SerialName("group_id") val groupId: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("branch_code") val branchCode: String = "",
    @SerialName("enabled") val enabled: Boolean = true,
    @SerialName("devices_count") val devicesCount: Int = 0,
    @SerialName("open_sessions_count") val openSessionsCount: Int = 0,
    @SerialName("has_open_session") val hasOpenSession: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
)

@Serializable
data class YappyOnsiteDeviceConfigRequest(
    @SerialName("device_id") val deviceId: String? = null,
    @SerialName("name") val name: String,
    @SerialName("user_code") val userCode: String? = null,
    @SerialName("billing_point") val billingPoint: String,
)

@Serializable
data class YappyOnsiteDevice(
    @Serializable(with = FlexibleStringSerializer::class)
    @SerialName("id") val id: String? = null,
    @SerialName("business_id") val businessId: Int? = null,
    @SerialName("group_id") val groupId: String = "",
    @SerialName("device_id") val deviceId: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("user_code") val userCode: String = "",
    @SerialName("branch_code") val branchCode: String = "",
    @SerialName("billing_point") val billingPoint: String = "",
    @SerialName("enabled") val enabled: Boolean = true,
    @SerialName("has_open_session") val hasOpenSession: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
)

@Serializable
data class YappyOnsiteCancelRequest(
    @SerialName("reason") val reason: String,
)

@Serializable
data class YappyOnsiteCancelPendingRequest(
    @SerialName("branch_code") val branchCode: String,
    @SerialName("billing_point") val billingPoint: String,
    @SerialName("reason") val reason: String,
)

@Serializable
data class YappyOnsiteCancelPendingResponse(
    @SerialName("cancelled") val cancelled: Boolean = false,
    @SerialName("transaction_id") val transactionId: String = "",
)

@Serializable
data class YappyOnsiteTransactionStatus(
    @SerialName("id") val id: Int? = null,
    @SerialName("transaction_id") val transactionId: String = "",
    @SerialName("session_id") val sessionId: String = "",
    @SerialName("status") val status: String = "",
    @SerialName("provider_status") val providerStatus: String = "",
    @SerialName("qr_type") val qrType: String = "",
    @SerialName("qr_hash") val qrHash: String = "",
    @SerialName("amount") val amount: String = "",
    @SerialName("currency") val currency: String = "USD",
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("cancelled_at") val cancelledAt: String? = null,
    @SerialName("returned_at") val returnedAt: String? = null,
)

@Serializable
data class YappyOnsiteOrderStatus(
    @SerialName("id") val id: Int? = null,
    @SerialName("order_number") val orderNumber: String = "",
    @SerialName("payment_flow_type") val paymentFlowType: String = "",
    @SerialName("order_status") val orderStatus: Int = 0,
    @SerialName("payment_status") val paymentStatus: Int = 0,
    @SerialName("invoice_status") val invoiceStatus: Int = 0,
    @SerialName("total_amount") val totalAmount: String = "",
    @SerialName("currency") val currency: String = "USD",
)

@Serializable
data class YappyOnsiteInvoiceStatus(
    @SerialName("status") val status: Int = 0,
    @SerialName("invoice_id") val invoiceId: String? = null,
    @SerialName("cufe") val cufe: String? = null,
    @SerialName("document_guid") val documentGuid: String? = null,
    @SerialName("auth_number") val authNumber: String? = null,
    @SerialName("invoiced_at") val invoicedAt: String? = null,
    @SerialName("ticket_enabled") val ticketEnabled: Boolean = false,
    @SerialName("ticket") val ticket: TicketDocumentPayload? = null,
    @SerialName("warning_code") val warningCode: String? = null,
    @SerialName("warning_message") val warningMessage: String? = null,
)

@Serializable
data class YappyOnsiteTransactionPayload(
    @SerialName("transaction") val transaction: YappyOnsiteTransactionStatus = YappyOnsiteTransactionStatus(),
    @SerialName("order") val order: YappyOnsiteOrderStatus = YappyOnsiteOrderStatus(),
    @SerialName("invoice") val invoice: YappyOnsiteInvoiceStatus = YappyOnsiteInvoiceStatus(),
)

object FlexibleStringSerializer : KSerializer<String?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: String?) {
        encoder.encodeString(value.orEmpty())
    }

    override fun deserialize(decoder: Decoder): String? {
        if (decoder !is JsonDecoder) return decoder.decodeString()
        val raw = decoder.decodeJsonElement().toString().trim('"')
        return raw.takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) }
    }
}
