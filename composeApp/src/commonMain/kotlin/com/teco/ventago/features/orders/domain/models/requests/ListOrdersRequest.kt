package com.teco.ventago.features.orders.domain.models.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ListOrdersRequest(
    @SerialName("business_id") val businessId: Int,
    val page: Int = 1,
    @SerialName("page_size") val pageSize: Int = 10,
    @SerialName("emission_start_date") val emissionStartDate: String? = null,
    @SerialName("emission_end_date") val emissionEndDate: String? = null,
    @SerialName("order_type") val orderType: String? = null,
    @SerialName("customer_ruc") val customerRuc: String? = null,
    @SerialName("payment_status") val paymentStatus: Int? = null,
    @SerialName("customer_id") val customerId: Long? = null,
    @SerialName("branch_code") val branchCode: String? = null,
    @SerialName("billing_point_code") val billingPointCode: String? = null,
)

@OptIn(ExperimentalSerializationApi::class)
val listOrdersRequestJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
    explicitNulls = false
}

fun ListOrdersRequest.toApiJsonString(): String {
    return listOrdersRequestJson.encodeToString(ListOrdersRequest.serializer(), this)
}

fun orderEmissionStartDate(dateIso: String): String? {
    return dateIso.toApiEmissionDate("T00:00:00-05:00")
}

fun orderEmissionEndDate(dateIso: String): String? {
    return dateIso.toApiEmissionDate("T23:59:59-05:00")
}

private fun String.toApiEmissionDate(timeSuffix: String): String? {
    return if (isBlank()) null else "$this$timeSuffix"
}
