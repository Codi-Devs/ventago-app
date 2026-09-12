package com.teco.ventago.features.expenses.domain

import com.teco.ventago.core.LocalStorage
import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.business.domain.BusinessService
import com.teco.ventago.features.expenses.data.repository.IExpensesRepository
import com.teco.ventago.features.expenses.domain.models.CrawlJob
import com.teco.ventago.features.expenses.domain.models.Expense
import com.teco.ventago.features.expenses.domain.models.ExpenseAccount
import com.teco.ventago.features.expenses.domain.models.ExpensePayment
import com.teco.ventago.features.expenses.domain.models.ExpensePaymentDeleteResult
import com.teco.ventago.features.expenses.domain.models.ExpensePaymentMutationResult
import com.teco.ventago.features.expenses.domain.models.PagedCrawlJobs
import com.teco.ventago.features.expenses.domain.models.PagedExpenses
import com.teco.ventago.features.expenses.domain.models.requests.CategorizeExpenseRequest
import com.teco.ventago.features.expenses.domain.models.requests.CreateExpenseAccountRequest
import com.teco.ventago.features.expenses.domain.models.requests.ExpenseProofFile
import com.teco.ventago.features.expenses.domain.models.requests.ListExpensesRequest
import com.teco.ventago.features.expenses.domain.models.requests.UpdateExpenseAccountRequest
import com.teco.ventago.features.expenses.domain.models.requests.UpsertExpenseRequest
import com.teco.ventago.features.expenses.domain.models.requests.UpsertExpensePaymentRequest
import com.teco.ventago.features.expenses.domain.models.ExpenseMerchant
import com.teco.ventago.features.expenses.domain.models.PagedMerchants
import com.teco.ventago.features.expenses.domain.models.requests.ListMerchantsRequest
import com.teco.ventago.features.expenses.domain.models.requests.CreateMerchantRequest
import com.teco.ventago.features.expenses.domain.models.requests.UpdateMerchantRequest
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

    fun replaceCache(expenses: List<Expense>) {
        saveCacheExpenses(expenses)
    }

    fun mergeExpensesIntoCache(fresh: List<Expense>): List<Expense> {
        val byId = LinkedHashMap<Long, Expense>()
        getCachedExpenses().forEach { expense ->
            val id = expense.id ?: return@forEach
            byId[id] = expense
        }
        for (expense in fresh) {
            val id = expense.id ?: continue
            byId[id] = mergeExpenseSnapshots(byId[id], expense) ?: expense
        }
        val merged = byId.values.toList()
        saveCacheExpenses(merged)
        return merged
    }

    fun upsertExpenseInCache(expense: Expense): Expense {
        val merged = mergeExpensesIntoCache(listOf(expense))
        return merged.firstOrNull { it.id == expense.id } ?: expense
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

    suspend fun getExpense(expenseId: Long, forceRefresh: Boolean = false): Expense {
        val businessId = businessId() ?: throw IllegalStateException("No business selected")
        if (!forceRefresh) {
            val cached = getCachedExpenses().firstOrNull { it.id == expenseId }
            if (cached != null && isExpenseSnapshotComplete(cached)) {
                return cached
            }
        }
        return try {
            val fetched = repository.getExpense(businessId, expenseId)
            upsertExpenseInCache(fetched)
        } catch (error: Exception) {
            getCachedExpenses().firstOrNull { it.id == expenseId } ?: throw error
        }
    }

    suspend fun createExpense(
        request: UpsertExpenseRequest,
        file: ExpenseProofFile? = null,
        paymentProofFiles: List<ExpenseProofFile> = emptyList()
    ): Expense {
        val businessId = businessId() ?: throw IllegalStateException("No business selected")
        val created = repository.createExpense(businessId, request, file, paymentProofFiles)
        return upsertExpenseInCache(created)
    }

    suspend fun updateExpense(
        expenseId: Long,
        request: UpsertExpenseRequest,
        file: ExpenseProofFile? = null
    ): Expense {
        val businessId = businessId() ?: throw IllegalStateException("No business selected")
        val updated = repository.updateExpense(businessId, expenseId, request, file)
        return upsertExpenseInCache(updated)
    }

    suspend fun deleteExpense(expenseId: Long): Boolean {
        val businessId = businessId() ?: throw IllegalStateException("No business selected")
        return repository.deleteExpense(businessId, expenseId)
    }

    suspend fun categorizeExpense(expenseId: Long, request: CategorizeExpenseRequest): Expense {
        val businessId = businessId() ?: throw IllegalStateException("No business selected")
        val categorized = repository.categorizeExpense(businessId, expenseId, request)
        return upsertExpenseInCache(categorized)
    }

    suspend fun getExpenseAccounts(includeInactive: Boolean = false): List<ExpenseAccount> {
        val businessId = businessId() ?: throw IllegalStateException("No business selected")
        return repository.getExpenseAccounts(businessId, includeInactive)
    }

    suspend fun createExpenseAccount(request: CreateExpenseAccountRequest): ExpenseAccount {
        val businessId = businessId() ?: throw IllegalStateException("No business selected")
        return repository.createExpenseAccount(businessId, request)
    }

    suspend fun updateExpenseAccount(
        accountId: Long,
        request: UpdateExpenseAccountRequest
    ): ExpenseAccount {
        val businessId = businessId() ?: throw IllegalStateException("No business selected")
        return repository.updateExpenseAccount(businessId, accountId, request)
    }

    suspend fun deactivateExpenseAccount(accountId: Long): Boolean {
        val businessId = businessId() ?: throw IllegalStateException("No business selected")
        return repository.deactivateExpenseAccount(businessId, accountId)
    }

    fun getBusinessId(): Int? = businessId()

    fun getBusinessName(): String? = businessService.business.value?.name
    fun getBusinessRuc(): String? = businessService.business.value?.ruc

    // Payments

    suspend fun createPayment(
        expenseId: Long,
        request: UpsertExpensePaymentRequest,
        proofFile: ExpenseProofFile? = null
    ): ExpensePaymentMutationResult {
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
    ): ExpensePaymentMutationResult {
        val businessId = businessId() ?: throw IllegalStateException("No business selected")
        return repository.updatePayment(businessId, expenseId, paymentId, request, proofFile)
    }

    suspend fun deletePayment(expenseId: Long, paymentId: Long): ExpensePaymentDeleteResult {
        val businessId = businessId() ?: throw IllegalStateException("No business selected")
        return repository.deletePayment(businessId, expenseId, paymentId)
    }

    // Merchants

    suspend fun listMerchants(request: ListMerchantsRequest): PagedMerchants {
        val businessId = businessId() ?: throw IllegalStateException("No business selected")
        return repository.listMerchants(businessId, request)
    }

    suspend fun searchMerchants(search: String, page: Int = 1, pageSize: Int = 20): List<ExpenseMerchant> {
        val businessId = businessId() ?: throw IllegalStateException("No business selected")
        val result = repository.listMerchants(businessId, ListMerchantsRequest(
            page = page,
            pageSize = pageSize,
            search = search,
            includeInactive = false
        ))
        return result.merchants
    }

    suspend fun getMerchant(merchantId: Long): ExpenseMerchant {
        val businessId = businessId() ?: throw IllegalStateException("No business selected")
        return repository.getMerchant(businessId, merchantId)
    }

    suspend fun createMerchant(request: CreateMerchantRequest): ExpenseMerchant {
        val businessId = businessId() ?: throw IllegalStateException("No business selected")
        return repository.createMerchant(businessId, request)
    }

    suspend fun updateMerchant(merchantId: Long, request: UpdateMerchantRequest): ExpenseMerchant {
        val businessId = businessId() ?: throw IllegalStateException("No business selected")
        return repository.updateMerchant(businessId, merchantId, request)
    }

    suspend fun deactivateMerchant(merchantId: Long): Boolean {
        val businessId = businessId() ?: throw IllegalStateException("No business selected")
        return repository.deactivateMerchant(businessId, merchantId)
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
        val merged = upsertExpenseInCache(expense)
        expenseUpdates.tryEmit(merged)
    }
}
