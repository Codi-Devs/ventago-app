package com.teco.ventago.features.customers.domain.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class CustomerListItem(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
    @SerialName("email") val email: String? = null,
    @SerialName("ruc") val ruc: String? = null,
    @SerialName("status") val status: Int,
    @SerialName("invoice_customer") val invoiceCustomer: Int,
    @SerialName("updated_at") val updatedAt: Long,
    @JsonNames("taxExempt")
    @SerialName("tax_exempt")
    @Serializable(with = FlexibleBooleanSerializer::class)
    val taxExempt: Boolean = false,
    @JsonNames("taxRetentionCode")
    @SerialName("tax_retention_code")
    @Serializable(with = FlexibleNullableIntSerializer::class)
    val taxRetentionCode: Int? = null,
    @JsonNames("taxRetentionPercent")
    @SerialName("tax_retention_percent")
    @Serializable(with = FlexibleNullableIntSerializer::class)
    val taxRetentionPercent: Int? = null,
)
