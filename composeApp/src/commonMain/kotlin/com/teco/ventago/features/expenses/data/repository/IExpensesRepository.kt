package com.teco.ventago.features.expenses.data.repository

import com.teco.ventago.features.expenses.domain.models.CrawlJob
import com.teco.ventago.features.expenses.domain.models.ExpenseAccount
import com.teco.ventago.features.expenses.domain.models.Expense
import com.teco.ventago.features.expenses.domain.models.ExpensePayment
import com.teco.ventago.features.expenses.domain.models.PagedCrawlJobs
import com.teco.ventago.features.expenses.domain.models.PagedExpenses
import com.teco.ventago.features.expenses.domain.models.requests.CategorizeExpenseRequest
import com.teco.ventago.features.expenses.domain.models.requests.CreateExpenseAccountRequest
import com.teco.ventago.features.expenses.domain.models.requests.ExpenseProofFile
import com.teco.ventago.features.expenses.domain.models.requests.ListExpensesRequest
import com.teco.ventago.features.expenses.domain.models.requests.UpsertExpenseRequest
import com.teco.ventago.features.expenses.domain.models.requests.UpsertExpensePaymentRequest
import com.teco.ventago.features.expenses.domain.models.requests.UpdateExpenseAccountRequest
import com.teco.ventago.features.expenses.domain.models.ExpenseMerchant
import com.teco.ventago.features.expenses.domain.models.PagedMerchants
import com.teco.ventago.features.expenses.domain.models.requests.ListMerchantsRequest
import com.teco.ventago.features.expenses.domain.models.requests.CreateMerchantRequest
import com.teco.ventago.features.expenses.domain.models.requests.UpdateMerchantRequest

interface IExpensesRepository {
    suspend fun listExpenses(businessId: Int, request: ListExpensesRequest): PagedExpenses
    suspend fun getExpense(businessId: Int, expenseId: Long): Expense
    suspend fun createExpense(
        businessId: Int,
        request: UpsertExpenseRequest,
        file: ExpenseProofFile? = null,
        paymentProofFiles: List<ExpenseProofFile> = emptyList()
    ): Expense
    suspend fun updateExpense(
        businessId: Int,
        expenseId: Long,
        request: UpsertExpenseRequest,
        file: ExpenseProofFile? = null
    ): Expense
    suspend fun deleteExpense(businessId: Int, expenseId: Long): Boolean
    suspend fun categorizeExpense(
        businessId: Int,
        expenseId: Long,
        request: CategorizeExpenseRequest
    ): Expense
    suspend fun getExpenseAccounts(businessId: Int, includeInactive: Boolean): List<ExpenseAccount>
    suspend fun createExpenseAccount(
        businessId: Int,
        request: CreateExpenseAccountRequest
    ): ExpenseAccount
    suspend fun updateExpenseAccount(
        businessId: Int,
        accountId: Long,
        request: UpdateExpenseAccountRequest
    ): ExpenseAccount
    suspend fun deactivateExpenseAccount(businessId: Int, accountId: Long): Boolean

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

    // Merchants
    suspend fun listMerchants(businessId: Int, request: ListMerchantsRequest): PagedMerchants
    suspend fun getMerchant(businessId: Int, merchantId: Long): ExpenseMerchant
    suspend fun createMerchant(businessId: Int, request: CreateMerchantRequest): ExpenseMerchant
    suspend fun updateMerchant(businessId: Int, merchantId: Long, request: UpdateMerchantRequest): ExpenseMerchant
    suspend fun deactivateMerchant(businessId: Int, merchantId: Long): Boolean

    // Crawl jobs
    suspend fun crawlExpense(businessId: Int, payload: String): CrawlJob
    suspend fun getCrawlJobStatus(businessId: Int, jobId: Long): CrawlJob
    suspend fun listCrawlJobs(businessId: Int, page: Int, pageSize: Int): PagedCrawlJobs
}
