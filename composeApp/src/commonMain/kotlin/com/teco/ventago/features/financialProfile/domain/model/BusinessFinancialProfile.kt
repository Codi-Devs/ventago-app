package com.teco.ventago.features.financialProfile.domain.model


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BusinessFinancialProfile(
    @SerialName("business_id") val businessId: Int,
    @SerialName("invoicing_active") val invoicingActive: Boolean,
    @SerialName("invoice_plan") val invoiceSummary: InvoiceSummary? = null,
    @SerialName("payment_summary") val paymentSummary: PaymentSummary
)

@Serializable
data class InvoiceSummary(
    @SerialName("plan_total_dte") val planTotalDte: Int,
    @SerialName("plan_available_dte") val planAvailableDte: Int,
    @SerialName("plan_start_date") val planStartDate: String,
    @SerialName("plan_expiry_date") val planExpiryDate: String
)

@Serializable
data class PaymentSummary(
    @SerialName("onboarding_completed") val onboardingCompleted: Boolean,

    @SerialName("pending_charges") val pendingCharges: Long,

    @SerialName("next_billing_date") val nextBillingDate: String, // ISO 8601 format assumed

    @SerialName("payment_methods") val paymentMethods: PaymentMethods,

    @SerialName("linked_paypal_billing_agreement") val linkedPaypalBillingAgreement: Boolean
)


@Serializable
data class PaymentMethods(
    @SerialName("paypal") val paypal: PaypalMethod,

    @SerialName("yappy") val yappy: YappyMethod,

    @SerialName("manual_transference") val manualTransference: ManualTransferenceMethod
)

@Serializable
data class PaypalMethod(
    @SerialName("visible") val visible: Boolean,

    @SerialName("linked_account") val linkedAccount: Boolean,

    @SerialName("email") val email: String
)

@Serializable
data class YappyMethod(
    @SerialName("visible") val visible: Boolean,

    @SerialName("linked_account") val linkedAccount: Boolean
)

@Serializable
data class ManualTransferenceMethod(
    @SerialName("visible") val visible: Boolean,

    @SerialName("enabled") val enabled: Boolean,

    @SerialName("payment_instructions") val paymentInstructions: String
)