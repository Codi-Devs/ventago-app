package com.teco.ventago.features.payments.domain.models

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

data class PaypalSummary(
    val ordersEnabled: Boolean,
    val billingAgreementConnected: Boolean,
    val onboardingCompleted: Boolean,
    val commissionPercent: Int,
    val pendingCharges: Int,
    val pendingChargesCurrency: String,
    val nextBillingDate: String
) {
    companion object {
        fun fromMap(map: Map<String, JsonElement>): PaypalSummary {
            return PaypalSummary(
                map["orders_enabled"]!!.jsonPrimitive.boolean,
                map["billing_agreement_connected"]!!.jsonPrimitive.boolean,
                map["onboarding_completed"]!!.jsonPrimitive.boolean,
                map["commission_percent"]!!.jsonPrimitive.intOrNull ?: 200,
                map["pending_charges"]!!.jsonPrimitive.intOrNull ?: 0,
                map["pending_charges_currency"]!!.jsonPrimitive.content,
                map["next_billing_date"]!!.jsonPrimitive.content
            )
        }
    }
}