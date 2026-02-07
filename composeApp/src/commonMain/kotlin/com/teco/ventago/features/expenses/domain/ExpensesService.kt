package com.teco.ventago.features.expenses.domain

import com.teco.ventago.core.LocalStorage
import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.business.domain.BusinessService
import com.teco.ventago.features.expenses.data.repository.IExpensesRepository
import com.teco.ventago.features.expenses.domain.models.CrawlJob
import com.teco.ventago.features.expenses.domain.models.Expense
import com.teco.ventago.features.expenses.domain.models.ExpensePayment
import com.teco.ventago.features.expenses.domain.models.PagedCrawlJobs
import com.teco.ventago.features.expenses.domain.models.PagedExpenses
import com.teco.ventago.features.expenses.domain.models.requests.ExpenseProofFile
import com.teco.ventago.features.expenses.domain.models.requests.ListExpensesRequest
import com.teco.ventago.features.expenses.domain.models.requests.UpsertExpenseRequest
import com.teco.ventago.features.expenses.domain.models.requests.UpsertExpensePaymentRequest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ExpensesService(
    private val repository: IExpensesRepository,
    private val businessService: BusinessService,
    private val logger: ILoggerService,
    private val authService: IAuthService,
    private val storage: LocalStorage,
    private val json: Json,
) {
    private val cacheMutex = Mutex()
    private val inFlightRequests = mutableMapOf<String, Boolean>()
    private val pendingRefreshResults = mutableMapOf<String, PagedExpenses>()
    private var cacheHydrationCompleted = false
    private val expenseUpdates = MutableSharedFlow<Expense>(extraBufferCapacity = 64)

    val expenseUpdatesFlow: SharedFlow<Expense> = expenseUpdates.asSharedFlow()

    private fun businessId(): Int? = businessService.business.value?.businessId

    private fun cacheKey(): String {
        val bId = businessId() ?: return "expenses_cache_0"
        return "expenses_cache_$bId"
    }

    private fun buildRequestKey(request: ListExpensesRequest): String {
        return json.encodeToString(request)
    }

    fun getCachedExpenses(): List<Expense> {
        val cached = storage.string(cacheKey()) ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<Expense>>(cached)
        }.getOrDefault(emptyList())
    }

    private fun saveCacheExpenses(expenses: List<Expense>) {
        runCatching {
            val encoded = json.encodeToString(expenses)
            storage.set(cacheKey(), encoded)
        }
    }

    fun mergeExpensesIntoCache(fresh: List<Expense>): List<Expense> {
        val cached = getCachedExpenses().toMutableList()
        val existingIds = cached.mapNotNull { it.id }.toMutableSet()
        for (expense in fresh) {
            val id = expense.id ?: continue
            if (id in existingIds) {
                val index = cached.indexOfFirst { it.id == id }
                if (index >= 0) cached[index] = expense
            } else {
                cached.add(expense)
                existingIds.add(id)
            }
        }
        saveCacheExpenses(cached)
        return cached
    }

    fun consumePendingRefresh(requestKey: String): PagedExpenses? {
        return pendingRefreshResults.remove(requestKey)
    }

    fun storePendingRefresh(requestKey: String, result: PagedExpenses) {
        pendingRefreshResults[requestKey] = result
    }

    fun isCacheHydrated(): Boolean = cacheHydrationCompleted

    fun markCacheHydrated() {
        cacheHydrationCompleted = true
    }

    suspend fun listExpenses(request: ListExpensesRequest): PagedExpenses {
        val businessId = businessId() ?: throw IllegalStateException("No business selected")
        val requestKey = buildRequestKey(request)

        // Deduplicate in-flight requests
        if (inFlightRequests[requestKey] == true) {
            return PagedExpenses()
        }

        return try {
            inFlightRequests[requestKey] = true
            repository.listExpenses(businessId, request)
        } finally {
            inFlightRequests.remove(requestKey)
        }
    }

    suspend fun getExpense(expenseId: Long): Expense {
        val businessId = businessId() ?: throw IllegalStateException("No business selected")
        return repository.getExpense(businessId, expenseId)
    }

    suspend fun createExpense(
        request: UpsertExpenseRequest,
        file: ExpenseProofFile? = null,
        paymentProofFiles: List<ExpenseProofFile> = emptyList()
    ): Expense {
        val businessId = businessId() ?: throw IllegalStateException("No business selected")
        return repository.createExpense(businessId, request, file, paymentProofFiles)
    }

    suspend fun updateExpense(expenseId: Long, request: UpsertExpenseRequest): Expense {
        val businessId = businessId() ?: throw IllegalStateException("No business selected")
        return repository.updateExpense(businessId, expenseId, request)
    }

    suspend fun deleteExpense(expenseId: Long): Boolean {
        val businessId = businessId() ?: throw IllegalStateException("No business selected")
        return repository.deleteExpense(businessId, expenseId)
    }

    fun getBusinessId(): Int? = businessId()

    fun getBusinessName(): String? = businessService.business.value?.name
    fun getBusinessRuc(): String? = businessService.business.value?.ruc

    // Payments

    suspend fun createPayment(
        expenseId: Long,
        request: UpsertExpensePaymentRequest,
        proofFile: ExpenseProofFile? = null
    ): ExpensePayment {
        val businessId = businessId() ?: throw IllegalStateException("No business selected")
        return repository.createPayment(businessId, expenseId, request, proofFile)
    }

    suspend fun listPayments(expenseId: Long): List<ExpensePayment> {
        val businessId = businessId() ?: throw IllegalStateException("No business selected")
        return repository.listPayments(businessId, expenseId)
    }

    suspend fun updatePayment(
        expenseId: Long,
        paymentId: Long,
        request: UpsertExpensePaymentRequest,
        proofFile: ExpenseProofFile? = null
    ): ExpensePayment {
        val businessId = businessId() ?: throw IllegalStateException("No business selected")
        return repository.updatePayment(businessId, expenseId, paymentId, request, proofFile)
    }

    suspend fun deletePayment(expenseId: Long, paymentId: Long): Boolean {
        val businessId = businessId() ?: throw IllegalStateException("No business selected")
        return repository.deletePayment(businessId, expenseId, paymentId)
    }

    // Crawl jobs (CUFE import)

    suspend fun crawlExpense(cufe: String): CrawlJob {
        val businessId = businessId() ?: throw IllegalStateException("No business selected")
        val payload = """{"cufe":"$cufe","business_id":$businessId}"""
        return repository.crawlExpense(businessId, payload)
    }

    suspend fun getCrawlJobStatus(jobId: Long): CrawlJob {
        val businessId = businessId() ?: throw IllegalStateException("No business selected")
        return repository.getCrawlJobStatus(businessId, jobId)
    }

    suspend fun listCrawlJobs(page: Int = 1, pageSize: Int = 100): PagedCrawlJobs {
        val businessId = businessId() ?: throw IllegalStateException("No business selected")
        return repository.listCrawlJobs(businessId, page, pageSize)
    }

    fun clearCache() {
        storage.deleteObject(cacheKey())
        pendingRefreshResults.clear()
        inFlightRequests.clear()
        cacheHydrationCompleted = false
    }

    fun publishExpenseUpdate(expense: Expense) {
        mergeExpensesIntoCache(listOf(expense))
        expenseUpdates.tryEmit(expense)
    }
}
