package com.teco.ventago.features.expenses.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExpenseMerchant(
    val id: Long,
    @SerialName("business_id") val businessId: Int? = null,
    val name: String,
    val ruc: String? = null,
    val dv: String? = null,
    val address: String? = null,
    val phone: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("updated_by") val updatedBy: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class PagedMerchants(
    val merchants: List<ExpenseMerchant> = emptyList(),
    val total: Long? = null,
    val page: Int? = null,
    @SerialName("page_size") val pageSize: Int? = null,
    @SerialName("total_pages") val totalPages: Int? = null
)
