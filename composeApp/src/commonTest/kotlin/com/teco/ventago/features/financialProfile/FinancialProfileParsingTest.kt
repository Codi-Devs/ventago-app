package com.teco.ventago.features.financialProfile

import com.teco.ventago.features.financialProfile.domain.model.BusinessFinancialProfile
import com.teco.ventago.utils.ApiResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

class FinancialProfileParsingTest {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun newConfigSummaryPayloadDecodesActiveSubscriptionsAndPaymentDefaults() {
        val profile = decodeProfile(
            """
            {
              "success": true,
              "data": {
                "business_id": 4,
                "payment_summary": {
                  "onboarding_completed": true,
                  "next_billing_date": "2026-06-15T03:00:00Z",
                  "fee_billing": {
                    "currency_code": "USD",
                    "pending_due_amount": "0.00",
                    "overdue_amount": "1.89",
                    "accrued_current_period_amount": "0.00",
                    "paid_amount": "0.00",
                    "next_batch_generation_at": "2026-07-01T05:10:00Z",
                    "next_due_at": "2026-06-16T04:59:59Z"
                  },
                  "payment_methods": {
                    "paypal": {
                      "visible": true,
                      "linked_account": false,
                      "email": ""
                    },
                    "yappy": {
                      "visible": true,
                      "linked_account": false
                    },
                    "ach": {
                      "visible": true,
                      "configured": true
                    },
                    "manual_transference": {
                      "visible": true,
                      "enabled": true,
                      "payment_instructions": ""
                    }
                  },
                  "linked_paypal_billing_agreement": false,
                  "auto_invoice_on_payment_success": false
                },
                "invoicing_active": true,
                "invoice_plan": {
                  "plan_total_dte": 2404,
                  "plan_available_dte": 1542,
                  "active_subscriptions": [
                    {
                      "subscription_id": 1,
                      "plan_id": 1,
                      "plan_name": "150 folios",
                      "initial_dte": 2400,
                      "available_dte": 1538,
                      "activation_date": "2025-11-03",
                      "expiry_date": "2028-01-21"
                    },
                    {
                      "subscription_id": 95,
                      "plan_id": 12,
                      "plan_name": "1 folio",
                      "initial_dte": 1,
                      "available_dte": 1,
                      "activation_date": "2026-05-15",
                      "expiry_date": "2027-05-15"
                    },
                    {
                      "subscription_id": 101,
                      "plan_id": 12,
                      "plan_name": "1 folio",
                      "initial_dte": 1,
                      "available_dte": 1,
                      "activation_date": "2026-05-28",
                      "expiry_date": "2027-05-28"
                    },
                    {
                      "subscription_id": 102,
                      "plan_id": 12,
                      "plan_name": "1 folio",
                      "initial_dte": 1,
                      "available_dte": 1,
                      "activation_date": "2026-05-28",
                      "expiry_date": "2027-05-28"
                    },
                    {
                      "subscription_id": 103,
                      "plan_id": 12,
                      "plan_name": "1 folio",
                      "initial_dte": 1,
                      "available_dte": 1,
                      "activation_date": "2026-05-29",
                      "expiry_date": "2027-05-29"
                    }
                  ]
                }
              },
              "error": null
            }
            """.trimIndent()
        )

        assertEquals(4, profile.businessId)
        assertTrue(profile.invoicingActive)
        assertEquals(5, profile.invoiceSummary?.activeSubscriptions?.size)
        assertEquals("2025-11-03", profile.invoiceSummary?.aggregateActivationDate())
        assertEquals("2028-01-21", profile.invoiceSummary?.aggregateExpiryDate())
        assertEquals(189L, profile.paymentSummary.feeBilling.overdueAmount)
        assertEquals(0L, profile.paymentSummary.pendingCharges)
        assertTrue(profile.paymentSummary.paymentMethods.ach.enabled)
    }

    @Test
    fun legacyCachedPayloadDecodesAndUsesLegacyPlanDates() {
        val profile = decodeProfile(
            """
            {
              "success": true,
              "data": {
                "business_id": 4,
                "payment_summary": {},
                "invoicing_active": true,
                "invoice_plan": {
                  "plan_total_dte": 150,
                  "plan_available_dte": 42,
                  "plan_start_date": "2025-11-03",
                  "plan_expiry_date": "2026-11-03"
                }
              },
              "error": null
            }
            """.trimIndent()
        )

        assertTrue(profile.invoiceSummary?.activeSubscriptions?.isEmpty() == true)
        assertEquals("2025-11-03", profile.invoiceSummary?.aggregateActivationDate())
        assertEquals("2026-11-03", profile.invoiceSummary?.aggregateExpiryDate())
    }

    private fun decodeProfile(payload: String): BusinessFinancialProfile {
        val body = Json.parseToJsonElement(payload) as JsonObject
        val response = ApiResponse.fromJson(body)
        assertTrue(response.successful)
        return json.decodeFromJsonElement(response.data!!)
    }
}
