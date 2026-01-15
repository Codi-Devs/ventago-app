package com.teco.ventago.features.quotes.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OtiTax(
    val code: String? = null,
    val rate: Double? = null
)

@Serializable
data class QuoteLine(
    @SerialName("item_id") val itemId: Long? = null,
    @SerialName("item_name") val itemName: String? = null,
    val quantity: Double? = null,
    @SerialName("unit_price") val unitPrice: Double? = null,
    @SerialName("discount_mode") val discountMode: Int? = null,
    @SerialName("discount_value") val discountValue: Double? = null,
    @SerialName("tax_name") val taxName: String? = null,
    @SerialName("tax_rate") val taxRate: String? = null,
    @SerialName("isc_rate") val iscRate: Double? = null,
    @SerialName("oti_taxes") val otiTaxes: List<OtiTax>? = null,
    @SerialName("line_total") val lineTotal: Double? = null,
    @SerialName("product_type") val productType: String? = null
)
