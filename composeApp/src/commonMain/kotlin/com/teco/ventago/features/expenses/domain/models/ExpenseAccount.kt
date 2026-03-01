package com.teco.ventago.features.expenses.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExpenseAccount(
    val id: Long,
    @SerialName("business_id") val businessId: Long? = null,
    @SerialName("parent_id") val parentId: Long? = null,
    val code: String,
    val name: String,
    val kind: String = "expense",
    @SerialName("is_system") val isSystem: Boolean = false,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)
