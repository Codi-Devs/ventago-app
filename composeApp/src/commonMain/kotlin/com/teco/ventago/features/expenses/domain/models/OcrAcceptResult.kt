package com.teco.ventago.features.expenses.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OcrAcceptResult(
    val accepted: Boolean = false,
    val route: String? = null,
    @SerialName("job_id") val jobId: Long? = null,
    @SerialName("expense_id") val expenseId: Long? = null,
    val cufe: String? = null,
    val message: String? = null
)
