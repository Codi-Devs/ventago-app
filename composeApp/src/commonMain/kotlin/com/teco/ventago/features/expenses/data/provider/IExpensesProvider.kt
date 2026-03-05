package com.teco.ventago.features.expenses.data.provider

import com.teco.ventago.features.expenses.domain.models.requests.ListExpensesRequest
import com.teco.ventago.features.expenses.domain.models.requests.ExpenseProofFile
import com.teco.ventago.features.expenses.domain.models.requests.CategorizeExpenseRequest
import com.teco.ventago.features.expenses.domain.models.requests.CreateExpenseAccountRequest
import com.teco.ventago.features.expenses.domain.models.requests.UpsertExpenseRequest
import com.teco.ventago.features.expenses.domain.models.requests.UpsertExpensePaymentRequest
import com.teco.ventago.features.expenses.domain.models.requests.UpdateExpenseAccountRequest
import com.teco.ventago.features.expenses.domain.models.requests.ListMerchantsRequest
import com.teco.ventago.features.expenses.domain.models.requests.CreateMerchantRequest
import com.teco.ventago.features.expenses.domain.models.requests.UpdateMerchantRequest
import com.teco.ventago.utils.ApiResponse

interface IExpensesProvider {
    suspend fun listExpenses(businessId: Int, request: ListExpensesRequest): ApiResponse
    suspend fun getExpense(businessId: Int, expenseId: Long): ApiResponse
    suspend fun createExpense(
        businessId: Int,
        request: UpsertExpenseRequest,
        file: ExpenseProofFile? = null,
        paymentProofFiles: List<ExpenseProofFile> = emptyList()
    ): ApiResponse
    suspend fun updateExpense(
        businessId: Int,
        expenseId: Long,
        request: UpsertExpenseRequest,
        file: ExpenseProofFile? = null
    ): ApiResponse
    suspend fun deleteExpense(businessId: Int, expenseId: Long): ApiResponse
    suspend fun categorizeExpense(
        businessId: Int,
        expenseId: Long,
        request: CategorizeExpenseRequest
    ): ApiResponse
    suspend fun getExpenseAccounts(businessId: Int, includeInactive: Boolean): ApiResponse
    suspend fun createExpenseAccount(businessId: Int, request: CreateExpenseAccountRequest): ApiResponse
    suspend fun updateExpenseAccount(
        businessId: Int,
        accountId: Long,
        request: UpdateExpenseAccountRequest
    ): ApiResponse
    suspend fun deactivateExpenseAccount(businessId: Int, accountId: Long): ApiResponse

    // Payments
    suspend fun createPayment(
        businessId: Int,
        expenseId: Long,
        request: UpsertExpensePaymentRequest,
        proofFile: ExpenseProofFile? = null
    ): ApiResponse
    suspend fun listPayments(businessId: Int, expenseId: Long): ApiResponse
    suspend fun updatePayment(
        businessId: Int,
        expenseId: Long,
        paymentId: Long,
        request: UpsertExpensePaymentRequest,
        proofFile: ExpenseProofFile? = null
    ): ApiResponse
    suspend fun deletePayment(businessId: Int, expenseId: Long, paymentId: Long): ApiResponse

    // Crawl jobs (CUFE import)
    suspend fun crawlExpense(businessId: Int, payload: String): ApiResponse
    suspend fun getCrawlJobStatus(businessId: Int, jobId: Long): ApiResponse
    suspend fun listCrawlJobs(businessId: Int, page: Int, pageSize: Int): ApiResponse

    // Merchants
    suspend fun listMerchants(businessId: Int, request: ListMerchantsRequest): ApiResponse
    suspend fun getMerchant(businessId: Int, merchantId: Long): ApiResponse
    suspend fun createMerchant(businessId: Int, request: CreateMerchantRequest): ApiResponse
    suspend fun updateMerchant(businessId: Int, merchantId: Long, request: UpdateMerchantRequest): ApiResponse
    suspend fun deactivateMerchant(businessId: Int, merchantId: Long): ApiResponse

    // Excel import
    suspend fun importExpenses(businessId: Int, payload: String): ApiResponse
    suspend fun listImports(businessId: Int): ApiResponse
}
