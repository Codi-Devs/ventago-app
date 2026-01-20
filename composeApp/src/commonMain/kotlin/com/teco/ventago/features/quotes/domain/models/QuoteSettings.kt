package com.teco.ventago.features.quotes.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QuoteSettings(
    @SerialName("default_quote_style") val defaultQuoteStyle: String = "style1",
    @SerialName("default_additional_info") val defaultAdditionalInfo: String = "",
    @SerialName("quote_prefix") val quotePrefix: String = "COT",
    @SerialName("default_include_payment_button") val defaultIncludePaymentButton: Boolean = false
)
