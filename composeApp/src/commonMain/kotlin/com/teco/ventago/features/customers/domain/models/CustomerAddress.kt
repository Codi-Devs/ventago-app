package com.teco.ventago.features.customers.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CustomerAddress(
    @SerialName("id") val id: Long,
    @SerialName("customer_id") val customerId: Long,
    @SerialName("address_line") val addressLine: String,
    @SerialName("location_code") val locationCode: String? = null,
    @SerialName("province") val province: String? = null,
    @SerialName("district") val district: String? = null,
    @SerialName("corregimiento") val corregimiento: String? = null,
    @SerialName("is_default") val isDefault: Boolean = false,
)
