package com.teco.ventago.features.expenses.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CrawlJob(
    val id: Long? = null,
    val cufe: String? = null,
    val status: String? = null,
    @SerialName("expense_id") val expenseId: Long? = null,
    @SerialName("error_message") val errorMessage: String? = null,
    @SerialName("attempt_count") val attemptCount: Int? = null,
    @SerialName("max_attempts") val maxAttempts: Int? = null,
    @SerialName("next_attempt_at") val nextAttemptAt: String? = null,
    @SerialName("last_attempt_at") val lastAttemptAt: String? = null,
    val message: String? = null,
    @SerialName("created_at") val createdAt: String? = null
) {
    val isTerminal: Boolean
        get() = status == "success" || status == "failed"
}

@Serializable
data class PagedCrawlJobs(
    val jobs: List<CrawlJob> = emptyList(),
    val total: Long? = null,
    val page: Int? = null,
    @SerialName("page_size") val pageSize: Int? = null,
    @SerialName("total_pages") val totalPages: Int? = null
)
