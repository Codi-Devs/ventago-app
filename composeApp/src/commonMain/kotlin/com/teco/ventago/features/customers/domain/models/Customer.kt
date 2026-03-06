package com.teco.ventago.features.customers.domain.models

import com.teco.ventago.features.invoicing.domain.models.FeCustomerType
import com.teco.ventago.features.invoicing.domain.TaxPayerType
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Customer(
    val id: Int,
    val name: String,
    val phone: String?,
    val email: String?,
    val ruc: String?,
    val invoiceCustomer: Boolean,
    val rucCheckDigit: String?,
    val tags: List<String>,
    val customerType: FeCustomerType?,
    val taxPayerType: TaxPayerType?,
    val addressLine: String?,
    val province: String?,
    val district: String?,
    val corregimiento: String?,
    val locationCode: String?,
    val foreignIdType: String?,
    val foreignIdNumber: String?,
    val cedulaCF: String?,
    val countryCode: String = "PA",
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
) {


}
