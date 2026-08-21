package com.teco.ventago.features.financialProfile.domain.model

import kotlin.math.roundToLong
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder

@Serializable
data class BusinessFinancialProfile(
    @SerialName("business_id") val businessId: Int,
    @SerialName("invoicing_active") val invoicingActive: Boolean,
    @SerialName("invoice_plan") val invoiceSummary: InvoiceSummary? = null,
    @SerialName("payment_summary") val paymentSummary: PaymentSummary,
)

@Serializable
data class InvoiceSummary(
    @SerialName("plan_total_dte") val planTotalDte: Int,
    @SerialName("plan_available_dte") val planAvailableDte: Int,
    @SerialName("plan_start_date") val planStartDate: String? = null,
    @SerialName("plan_expiry_date") val planExpiryDate: String? = null,
    @SerialName("active_subscriptions") val activeSubscriptions: List<InvoiceSubscriptionSummary> = emptyList(),
) {
    fun aggregateActivationDate(): String =
        activeSubscriptions
            .map(InvoiceSubscriptionSummary::activationDate)
            .filter(String::isNotBlank)
            .minOrNull()
            ?: planStartDate.orEmpty()

    fun aggregateExpiryDate(): String =
        activeSubscriptions
            .map(InvoiceSubscriptionSummary::expiryDate)
            .filter(String::isNotBlank)
            .maxOrNull()
            ?: planExpiryDate.orEmpty()
}

@Serializable
data class InvoiceSubscriptionSummary(
    @SerialName("subscription_id") val subscriptionId: Int,
    @SerialName("plan_id") val planId: Int,
    @SerialName("plan_name") val planName: String,
    @SerialName("initial_dte") val initialDte: Int,
    @SerialName("available_dte") val availableDte: Int,
    @SerialName("activation_date") val activationDate: String,
    @SerialName("expiry_date") val expiryDate: String,
)

@Serializable
data class PaymentSummary(
    @SerialName("onboarding_completed") val onboardingCompleted: Boolean = false,
    @Serializable(with = MoneyAmountToCentsSerializer::class)
    @SerialName("pending_charges") val pendingCharges: Long = 0L,
    @SerialName("next_billing_date") val nextBillingDate: String = "",
    @SerialName("payment_methods") val paymentMethods: PaymentMethods = PaymentMethods(),
    @SerialName("linked_paypal_billing_agreement") val linkedPaypalBillingAgreement: Boolean = false,
    @SerialName("auto_invoice_on_payment_success") val autoInvoiceOnPaymentSuccess: Boolean = false,
    @SerialName("fee_billing") val feeBilling: FeeBillingSummary = FeeBillingSummary(),
    @SerialName("module_access") val moduleAccess: PaymentModuleAccess = PaymentModuleAccess(),
)

@Serializable
data class PaymentModuleAccess(
    @SerialName("enabled") val enabled: Boolean = false,
    @SerialName("source") val source: String = "none",
    @SerialName("plan_code") val planCode: String = "payments_module",
    @SerialName("entitlement_key") val entitlementKey: String = "payments_module",
    @SerialName("subscription") val subscription: PaymentModuleAccessSubscription? = null,
) {
    fun hasAccess(): Boolean = enabled && (source == "subscription" || source == "beta")
}

@Serializable
data class PaymentModuleAccessSubscription(
    @SerialName("id") val id: String = "",
    @SerialName("status") val status: String = "",
    @SerialName("billing_period") val billingPeriod: String = "",
    @SerialName("current_period_end") val currentPeriodEnd: String = "",
    @SerialName("next_renewal_at") val nextRenewalAt: String = "",
    @SerialName("cancel_at_period_end") val cancelAtPeriodEnd: Boolean = false,
)

@Serializable
data class PaymentMethods(
    @SerialName("paypal") val paypal: PaypalMethod = PaypalMethod(),
    @SerialName("yappy") val yappy: YappyMethod = YappyMethod(),
    @SerialName("ach") val ach: AchMethod = AchMethod(),
    @SerialName("card") val card: CardMethod = CardMethod(),
    @SerialName("manual_transference") val manualTransference: ManualTransferenceMethod = ManualTransferenceMethod(),
)

@Serializable
data class PaypalMethod(
    @SerialName("visible") val visible: Boolean = false,
    @SerialName("configured") val configured: Boolean = false,
    @SerialName("enabled") val enabled: Boolean = false,
    @SerialName("linked_account") val linkedAccount: Boolean = false,
    @SerialName("email") val email: String = "",
) {
    fun readyForPayments(): Boolean = configured && enabled
}

@Serializable
data class YappyMethod(
    @SerialName("visible") val visible: Boolean = false,
    @SerialName("linked_account") val linkedAccount: Boolean = false,
    @SerialName("onsite") val onsite: YappyOnsiteSummary = YappyOnsiteSummary(),
)

@Serializable
data class YappyOnsiteSummary(
    @SerialName("configured") val configured: Boolean = false,
    @SerialName("enabled") val enabled: Boolean = false,
    @SerialName("groups_count") val groupsCount: Int = 0,
    @SerialName("devices_count") val devicesCount: Int = 0,
    @SerialName("open_sessions_count") val openSessionsCount: Int = 0,
    @SerialName("has_open_session") val hasOpenSession: Boolean = false,
)

@Serializable
data class ManualTransferenceMethod(
    @SerialName("visible") val visible: Boolean = false,
    @SerialName("enabled") val enabled: Boolean = false,
    @SerialName("payment_instructions") val paymentInstructions: String = "",
)

@Serializable
data class AchMethod(
    @SerialName("visible") val visible: Boolean = false,
    @SerialName("configured") val configured: Boolean = false,
    @SerialName("enabled") val enabled: Boolean = true,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("pending_review_count") val pendingReviewCount: Int = 0,
    @SerialName("account") val account: AchAccountSummary? = null,
)

@Serializable
data class AchAccountSummary(
    @SerialName("bank_name") val bankName: String = "",
    @SerialName("bank_code") val bankCode: String = "",
    @SerialName("account_type") val accountType: String = "",
    @SerialName("account_number") val accountNumber: String = "",
    @SerialName("account_number_masked") val accountNumberMasked: String = "",
    @SerialName("account_holder_name") val accountHolderName: String = "",
)

@Serializable
data class CardMethod(
    @SerialName("visible") val visible: Boolean = false,
    @SerialName("configured") val configured: Boolean = false,
    @SerialName("enabled") val enabled: Boolean = false,
    @SerialName("platform_allowed") val platformAllowed: Boolean = false,
    @SerialName("business_enabled") val businessEnabled: Boolean = false,
    @SerialName("providers") val providers: List<CardProviderStatus> = emptyList(),
) {
    fun readyForPayments(): Boolean {
        val methodReady = configured && enabled && platformAllowed && businessEnabled
        return methodReady || providers.any { it.readyForPayments() }
    }
}

@Serializable
data class CardProviderStatus(
    @SerialName("provider") val provider: String = "",
    @SerialName("payment_method") val paymentMethod: String = "",
    @SerialName("configured") val configured: Boolean = false,
    @SerialName("enabled") val enabled: Boolean = false,
    @SerialName("platform_allowed") val platformAllowed: Boolean = false,
    @SerialName("business_enabled") val businessEnabled: Boolean = false,
) {
    fun readyForPayments(): Boolean = configured && enabled && platformAllowed && businessEnabled
}

@Serializable
data class FeeBillingSummary(
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

object MoneyAmountToCentsSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("MoneyAmountToCents", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Long) {
        encoder.encodeLong(value)
    }

    override fun deserialize(decoder: Decoder): Long {
        if (decoder !is JsonDecoder) return decoder.decodeLong()
        val raw = decoder.decodeJsonElement().toString().trim('"')
        if (raw.isBlank() || raw == "null") return 0L

        val normalized = raw.replace(',', '.')
        val hasDecimals = normalized.contains('.')
        val asDouble = normalized.toDoubleOrNull()
        val asLong = normalized.toLongOrNull()

        return when {
            hasDecimals && asDouble != null -> (asDouble * 100.0).roundToLong()
            asLong != null -> asLong
            asDouble != null -> (asDouble * 100.0).roundToLong()
            else -> 0L
        }
    }
}
