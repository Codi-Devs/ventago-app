package com.teco.ventago.features.payments.domain.models

import com.teco.ventago.features.financialProfile.domain.model.MoneyAmountToCentsSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    @SerialName("pending_review_count") val pendingReviewCount: Int = 0,
    @SerialName("account") val account: AchAccount? = null,
)

@Serializable
data class AchAccountConfigRequest(
    @SerialName("bank_code") val bankCode: String,
    @SerialName("bank_name") val bankName: String,
    @SerialName("account_type") val accountType: String,
    @SerialName("account_number") val accountNumber: String,
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
    @SerialName("related_order_number") val relatedOrderNumber: String = "",
    @SerialName("order_id") val orderId: Long? = null,
    @SerialName("payment_method") val paymentMethod: String = "",
    @Serializable(with = MoneyAmountToCentsSerializer::class)
    @SerialName("fee_generated") val feeGenerated: Long = 0L,
    @SerialName("date") val date: String = "",
    @SerialName("fee_status") val feeStatus: String = "",
    @SerialName("billing_bucket") val billingBucket: String = "",
    @SerialName("batch_id") val batchId: Long? = null,
    @SerialName("batch_status") val batchStatus: String? = null,
)

@Serializable
data class FeeBatchItem(
    @SerialName("period_start") val periodStart: String = "",
    @SerialName("period_end") val periodEnd: String = "",
    @SerialName("status") val status: String = "",
    @SerialName("total_lines") val totalLines: Int = 0,
    @Serializable(with = MoneyAmountToCentsSerializer::class)
    @SerialName("platform_fee_total") val platformFeeTotal: Long = 0L,
    @Serializable(with = MoneyAmountToCentsSerializer::class)
    @SerialName("tenant_markup_total") val tenantMarkupTotal: Long = 0L,
    @Serializable(with = MoneyAmountToCentsSerializer::class)
    @SerialName("total_fee_total") val totalFeeTotal: Long = 0L,
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
)

@Serializable
data class FeeBatchesPayload(
    @SerialName("items") val items: List<FeeBatchItem> = emptyList(),
    @SerialName("pagination") val pagination: PaginationMeta = PaginationMeta(),
)
