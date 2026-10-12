package com.teco.ventago.features.orders.domain.models.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RetryInvoiceRequest(
    @SerialName("final_customer_country_code") val finalCustomerCountryCode: String? = null,
)
