package com.teco.ventago.features.expenses.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class ExpenseParty(
    val name: String? = null,
    val ruc: String? = null,
    val dv: String? = null,
    val type: String? = null,
    val address: String? = null,
    val phone: String? = null
)
