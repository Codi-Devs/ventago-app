package com.teco.ventago.features.expenses.domain.models

data class PagedExpenses(
    val expenses: List<Expense> = emptyList(),
    val total: Long? = null,
    val page: Int? = null,
    val size: Int? = null
)
