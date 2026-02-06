package com.teco.ventago.features.expenses.domain.models.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ListExpensesRequest(
    @SerialName("business_id") val businessId: Int,
    val page: Int = 1,
    @SerialName("page_size") val pageSize: Int = 10,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
    val source: String? = null,
    @SerialName("issuer_name") val issuerName: String? = null,
    @SerialName("issuer_ruc") val issuerRuc: String? = null,
    @SerialName("invoice_number") val invoiceNumber: String? = null
)
