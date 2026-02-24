package com.teco.ventago.features.customers.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ValidateRucRequest(
    @SerialName("ruc") val ruc: String
)

@Serializable
data class UpdateCustomerDetailsRequest(
    @SerialName("email") val email: String? = null,
    @SerialName("phone1") val phone1: String? = null,
    @SerialName("address_line") val addressLine: String? = null,
    @SerialName("location_code") val locationCode: String? = null,
)

@Serializable
data class CreateBillingAddressRequest(
    @SerialName("address_line") val addressLine: String,
    @SerialName("location_code") val locationCode: String? = null,
    @SerialName("email") val email: String? = null,
)

@Serializable
data class UpdateBillingAddressRequest(
    @SerialName("address_line") val addressLine: String,
    @SerialName("location_code") val locationCode: String? = null,
    @SerialName("email") val email: String? = null,
)
