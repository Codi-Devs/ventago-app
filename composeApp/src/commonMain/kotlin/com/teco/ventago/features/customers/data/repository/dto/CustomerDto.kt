package com.teco.ventago.features.customers.data.repository.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class CreateCustomerDto(
    @SerialName("name") val name: String,
    @SerialName("email") val email: String?,
    @SerialName("phone") val phone: String?,
    @SerialName("tax_id") val ruc: String?,
    @SerialName("country_code") val countryCode: String = "PA",
    @SerialName("tags") val tags: String?, // Comma separated tags

    @SerialName("fe_customer_type") val customerType: String?,
    @SerialName("taxpayer_type") val taxPayerType: String?,
    @SerialName("address_line") val addressLine: String?,
    @SerialName("location_code") val locationCode: String?,
    @SerialName("province") val province: String?,
    @SerialName("district") val district: String?,
    @SerialName("corregimiento") val corregimiento: String?,

    @SerialName("foreign_id_type") val foreignIdType: String?,
    @SerialName("foreign_id_number") val foreignIdNumber: String?,
    @SerialName("cedula_cf") val cedulaCF: String?,
    @SerialName("country_other_name") val countryOtherName: String?,
    @SerialName("tax_exempt") val taxExempt: Boolean,
    @SerialName("tax_retention_code") val taxRetentionCode: Int? = null,
    @SerialName("tax_retention_percent") val taxRetentionPercent: Int? = null,
)

@Serializable
data class CustomerCreatedDto(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
    @SerialName("email") val email: String? = null,
    @SerialName("phone") val phone: String? = null,
    @SerialName("tax_id") val ruc: String? = null,
    @SerialName("country_code") val countryCode: String = "PA",
    @SerialName("tags") val tags: String? = null, // Comma separated tags
    @SerialName("tax_exempt") val taxExempt: Boolean = false,
    @SerialName("tax_retention_code") val taxRetentionCode: Int? = null,
    @SerialName("tax_retention_percent") val taxRetentionPercent: Int? = null,
)


@Serializable
data class CustomerListDto(
    val id: Int,
    val name: String,
    val email: String?,
    val ruc: String?,
    val status: Int,
    @SerialName("invoice_customer") val invoiceCustomer: Int,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("tax_exempt") val taxExempt: Boolean = false,
    @SerialName("tax_retention_code") val taxRetentionCode: Int? = null,
    @SerialName("tax_retention_percent") val taxRetentionPercent: Int? = null,
)
