package com.teco.ventago.features.quotes.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QuoteTotals(
    val subtotal: Double? = null,
    @SerialName("discount_total") val discount: Double? = null,
    @SerialName("tax_total") val taxes: Double? = null,
    @SerialName("total_amount") val total: Double? = null
)
