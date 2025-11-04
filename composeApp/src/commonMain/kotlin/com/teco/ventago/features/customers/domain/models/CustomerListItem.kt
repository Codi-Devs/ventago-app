package com.teco.ventago.features.customers.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CustomerListItem(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
    @SerialName("email") val email: String?,
    @SerialName("ruc") val ruc: String?,
    @SerialName("status") val status: Int,
    @SerialName("invoice_customer") val invoiceCustomer: Int,
    @SerialName("updated_at") val updatedAt: Long,
)