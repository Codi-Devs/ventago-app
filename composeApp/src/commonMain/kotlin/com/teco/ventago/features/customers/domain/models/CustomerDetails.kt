package com.teco.ventago.features.customers.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CustomerDetails(
    @SerialName("id") val id: Long,
    @SerialName("fe_customer_type") val feCustomerType: String? = null,
    @SerialName("taxpayer_type") val taxpayerType: String? = null,
    @SerialName("ruc_number") val rucNumber: String? = null,
    @SerialName("ruc_check_digit") val rucCheckDigit: String? = null,
    @SerialName("foreign_id_type") val foreignIdType: String? = null,
    @SerialName("foreign_id_number") val foreignIdNumber: String? = null,
    @SerialName("cedula_cf") val cedulaCf: String? = null,
    @SerialName("legal_name") val legalName: String? = null,
    @SerialName("address_line") val addressLine: String? = null,
    @SerialName("location_code") val locationCode: String? = null,
    @SerialName("province") val province: String? = null,
    @SerialName("district") val district: String? = null,
    @SerialName("corregimiento") val corregimiento: String? = null,
    @SerialName("country_code") val countryCode: String? = null,
    @SerialName("phone1") val phone1: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("status") val status: Int = 1,
)
