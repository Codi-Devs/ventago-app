package com.teco.ventago.features.payments.data.provider

import com.teco.ventago.utils.escapeJsonString


object PaypalRequests {

    fun businessIdRequest(businessId: Int): String =
        """
            {
                "business_id": $businessId
            }
        """.trimIndent()


    fun transference(businessId: Int, enabled: Boolean, instructions: String):String =
        """
            {
                "business_id": $businessId,
                "instructions": "${escapeJsonString(instructions)}",
                "enabled": $enabled
            }
        """.trimIndent()

    fun yappyConnect(businessId: Int, merchantID: String, domain: String, secretKey: String):String =
        """
            {
                "business_id": $businessId,
                "yappy_merchant_id": "$merchantID",
                "yappy_domain": "$domain",
                "yappy_secret_key": "$secretKey"
            }
        """.trimIndent()

    fun executeBillingAgreement(businessId: Int, token: String): String =
        """
            {
                "business_id": $businessId,
                "token": "$token"
            }
        """.trimIndent()
}