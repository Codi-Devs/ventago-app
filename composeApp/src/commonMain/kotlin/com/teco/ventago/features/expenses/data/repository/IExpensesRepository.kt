package com.teco.ventago.features.expenses.data.repository

import com.teco.ventago.features.expenses.domain.models.CrawlJob
import com.teco.ventago.features.expenses.domain.models.Expense
import com.teco.ventago.features.expenses.domain.models.ExpensePayment
import com.teco.ventago.features.expenses.domain.models.PagedCrawlJobs
import com.teco.ventago.features.expenses.domain.models.PagedExpenses
import com.teco.ventago.features.expenses.domain.models.requests.ExpenseProofFile
import com.teco.ventago.features.expenses.domain.models.requests.ListExpensesRequest
import com.teco.ventago.features.expenses.domain.models.requests.UpsertExpensePaymentRequest

interface IExpensesRepository {
    suspend fun listExpenses(businessId: Int, request: ListExpensesRequest): PagedExpenses
    suspend fun getExpense(businessId: Int, expenseId: Long): Expense
    suspend fun createExpense(businessId: Int, payload: String): Expense
    suspend fun updateExpense(businessId: Int, expenseId: Long, payload: String): Expense
    suspend fun deleteExpense(businessId: Int, expenseId: Long): Boolean

    // Payments
    suspend fun createPayment(
        businessId: Int,
        expenseId: Long,
        request: UpsertExpensePaymentRequest,
        proofFile: ExpenseProofFile? = null
    ): ExpensePayment
    suspend fun listPayments(businessId: Int, expenseId: Long): List<ExpensePayment>
    suspend fun updatePayment(
        businessId: Int,
        expenseId: Long,
        paymentId: Long,
        request: UpsertExpensePaymentRequest,
        proofFile: ExpenseProofFile? = null
    ): ExpensePayment
    suspend fun deletePayment(businessId: Int, expenseId: Long, paymentId: Long): Boolean

    // Crawl jobs
    suspend fun crawlExpense(businessId: Int, payload: String): CrawlJob
    suspend fun getCrawlJobStatus(businessId: Int, jobId: Long): CrawlJob
    suspend fun listCrawlJobs(businessId: Int, page: Int, pageSize: Int): PagedCrawlJobs
}
