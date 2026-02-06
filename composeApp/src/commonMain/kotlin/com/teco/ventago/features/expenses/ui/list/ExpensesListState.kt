package com.teco.ventago.features.expenses.ui.list

import com.teco.ventago.features.expenses.domain.models.CrawlJob
import com.teco.ventago.features.expenses.domain.models.Expense

data class ExpensesListState(
    val expenses: List<Expense> = emptyList(),
    val page: Int = 1,
    val pageSize: Int = 10,
    val isLoading: Boolean = false,
    val refreshing: Boolean = false,
    val isSyncing: Boolean = false,
    val noMore: Boolean = false,
    val error: String? = null,
    // Filters
    val startDate: String? = null,
    val endDate: String? = null,
    val invoiceNumber: String = "",
    val issuerName: String = "",
    val issuerRuc: String = "",
    val source: String? = null,
    val paymentStatuses: List<String> = emptyList(),
    // Search
    val searchQuery: String = "",
    // Feature flags
    val hasExpensesQr: Boolean = false,
    // Crawl jobs summary
    val crawlJobs: List<CrawlJob> = emptyList(),
    val isLoadingCrawlJobs: Boolean = false
)
