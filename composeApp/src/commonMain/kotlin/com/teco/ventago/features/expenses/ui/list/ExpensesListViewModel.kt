package com.teco.ventago.features.expenses.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.beta.BetaFeature
import com.teco.ventago.core.beta.BetaService
import com.teco.ventago.features.expenses.domain.ExpensesService
import com.teco.ventago.features.expenses.domain.models.Expense
import com.teco.ventago.features.expenses.domain.buildExpenseConceptLabel
import com.teco.ventago.features.expenses.domain.models.requests.ListExpensesRequest
import com.teco.ventago.features.expenses.domain.models.ExpenseMerchant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ExpensesListViewModel(
    private val expensesService: ExpensesService,
    private val betaService: BetaService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpensesListState())
    val uiState: StateFlow<ExpensesListState> = _uiState.asStateFlow()
    private var merchantSearchJob: Job? = null

    init {
        viewModelScope.launch {
            betaService.accessFlow(BetaFeature.EXPENSES_QR)
                .onEach { hasAccess ->
                    _uiState.value = _uiState.value.copy(hasExpensesQr = hasAccess)
                    if (hasAccess) loadCrawlJobs()
                }
                .launchIn(this)
        }
        viewModelScope.launch {
            expensesService.expenseUpdatesFlow
                .onEach { updated ->
                    val current = _uiState.value
                    val currentExpenses = current.expenses
                    val index = currentExpenses.indexOfFirst { it.id != null && it.id == updated.id }
                    if (index < 0) return@onEach

                    val merged = currentExpenses.toMutableList().also { it[index] = updated }
                    _uiState.value = current.copy(expenses = applyLocalFilters(merged, current))
                }
                .launchIn(this)
        }
        loadExpenses(refresh = true)
    }

    private fun loadCrawlJobs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingCrawlJobs = true)
            try {
                val result = withContext(Dispatchers.IO) {
                    expensesService.listCrawlJobs()
                }
                _uiState.value = _uiState.value.copy(
                    crawlJobs = result.jobs,
                    isLoadingCrawlJobs = false
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isLoadingCrawlJobs = false)
            }
        }
    }

    fun loadExpenses(refresh: Boolean = false) {
        viewModelScope.launch {
            val state = _uiState.value
            val currentPage = if (refresh) 1 else state.page

            val businessId = expensesService.getBusinessId() ?: return@launch

            val request = buildRequest(businessId, currentPage)
            val requestKey = Json.encodeToString(request)

            // Check for pending refresh result
            val pending = expensesService.consumePendingRefresh(requestKey)
            if (pending != null) {
                val merged = if (refresh) pending.expenses else state.expenses + pending.expenses
                val noMore = pending.expenses.isEmpty() ||
                    (pending.size != null && pending.expenses.size < (pending.size))
                _uiState.value = state.copy(
                    expenses = merged,
                    page = currentPage + 1,
                    isLoading = false,
                    refreshing = false,
                    isSyncing = false,
                    noMore = noMore,
                    error = null
                )
                return@launch
            }

            // Cache-first: on first load, show cached data and sync in background
            if (refresh && !expensesService.isCacheHydrated()) {
                val cached = expensesService.getCachedExpenses()
                if (cached.isNotEmpty()) {
                    _uiState.value = state.copy(
                        expenses = applyLocalFilters(cached, state),
                        isSyncing = true,
                        isLoading = false,
                        refreshing = false
                    )
                    // Background sync
                    backgroundSync(businessId, request, requestKey)
                    return@launch
                }
            }

            _uiState.value = state.copy(
                isLoading = refresh && state.expenses.isEmpty(),
                refreshing = refresh && state.expenses.isNotEmpty()
            )

            try {
                val response = withContext(Dispatchers.IO) {
                    expensesService.listExpenses(request)
                }

                expensesService.markCacheHydrated()

                // Update cache: replace on refresh, merge on pagination
                if (refresh) {
                    expensesService.replaceCache(response.expenses)
                } else if (response.expenses.isNotEmpty()) {
                    expensesService.mergeExpensesIntoCache(response.expenses)
                }

                val newExpenses = if (refresh) response.expenses else state.expenses + response.expenses
                val noMore = response.expenses.isEmpty() ||
                    (response.size != null && response.expenses.size < (response.size))

                _uiState.value = _uiState.value.copy(
                    expenses = newExpenses,
                    page = currentPage + 1,
                    isLoading = false,
                    refreshing = false,
                    isSyncing = false,
                    noMore = noMore,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    refreshing = false,
                    isSyncing = false,
                    error = e.message
                )
            }
        }
    }

    private fun backgroundSync(businessId: Int, request: ListExpensesRequest, requestKey: String) {
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    expensesService.listExpenses(request)
                }
                expensesService.markCacheHydrated()

                // Check if filters/page changed while syncing
                val currentState = _uiState.value
                val currentRequest = buildRequest(businessId, 1)
                val currentKey = Json.encodeToString(currentRequest)
                if (currentKey != requestKey) {
                    // Stale response, store for later
                    expensesService.storePendingRefresh(requestKey, response)
                    return@launch
                }

                // Replace cache with fresh data from API
                expensesService.replaceCache(response.expenses)
                val filtered = applyLocalFilters(response.expenses, currentState)
                val noMore = response.expenses.isEmpty() ||
                    (response.size != null && response.expenses.size < (response.size))

                _uiState.value = currentState.copy(
                    expenses = filtered,
                    page = 2,
                    isSyncing = false,
                    noMore = noMore,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSyncing = false)
            }
        }
    }

    private fun buildRequest(businessId: Int, page: Int): ListExpensesRequest {
        val state = _uiState.value
        return ListExpensesRequest(
            businessId = businessId,
            page = page,
            pageSize = state.pageSize,
            startDate = state.startDate?.let { formatStartDate(it) },
            endDate = state.endDate?.let { formatEndDate(it) },
            source = state.source,
            paymentStatus = state.paymentStatuses.takeIf { it.isNotEmpty() },
            issuerName = state.issuerName.ifBlank { null },
            issuerRuc = state.issuerRuc.ifBlank { null },
            invoiceNumber = state.searchQuery.ifBlank {
                state.invoiceNumber.ifBlank { null }
            },
            categorizationStatus = state.categorizationStatus,
            merchantId = state.merchantId
        )
    }

    private fun formatStartDate(date: String): String {
        val d = date.take(10)
        return if (d.contains("T")) d else "${d}T00:00:00-05:00"
    }

    private fun formatEndDate(date: String): String {
        val d = date.take(10)
        return if (d.contains("T")) d else "${d}T23:59:59-05:00"
    }

    private fun applyLocalFilters(expenses: List<Expense>, state: ExpensesListState): List<Expense> {
        var filtered = expenses

        if (state.source != null) {
            filtered = filtered.filter { it.source == state.source }
        }

        if (state.paymentStatuses.isNotEmpty()) {
            filtered = filtered.filter { it.paymentStatus in state.paymentStatuses }
        }

        if (state.categorizationStatus != null) {
            filtered = filtered.filter { it.categorizationStatus == state.categorizationStatus }
        }

        val search = state.searchQuery.ifBlank { state.invoiceNumber }
        if (search.isNotBlank()) {
            filtered = filtered.filter {
                it.invoiceNumber?.contains(search, ignoreCase = true) == true
            }
        }

        if (state.issuerName.isNotBlank()) {
            filtered = filtered.filter {
                it.issuer?.name?.contains(state.issuerName, ignoreCase = true) == true
            }
        }

        if (state.issuerRuc.isNotBlank()) {
            filtered = filtered.filter {
                val ruc = it.issuer?.ruc ?: ""
                val dv = it.issuer?.dv ?: ""
                val rucDv = "$ruc-$dv"
                ruc.contains(state.issuerRuc, ignoreCase = true) ||
                    rucDv.contains(state.issuerRuc, ignoreCase = true)
            }
        }

        return filtered
    }

    fun setSearchQuery(value: String) {
        _uiState.value = _uiState.value.copy(searchQuery = value)
    }

    fun search() {
        _uiState.value = _uiState.value.copy(page = 1, expenses = emptyList(), noMore = false)
        loadExpenses(refresh = true)
    }

    fun setStartDate(value: String?) {
        _uiState.value = _uiState.value.copy(startDate = value)
    }

    fun setEndDate(value: String?) {
        _uiState.value = _uiState.value.copy(endDate = value)
    }

    fun setSource(value: String?) {
        _uiState.value = _uiState.value.copy(source = value)
    }

    fun setIssuerName(value: String) {
        val wasSelected = _uiState.value.merchantId != null
        _uiState.value = _uiState.value.copy(
            issuerName = value,
            merchantId = if (wasSelected) null else _uiState.value.merchantId,
            merchantName = if (wasSelected) null else _uiState.value.merchantName
        )
        searchMerchantsForFilter(value)
    }

    fun selectFilterMerchant(merchant: ExpenseMerchant) {
        _uiState.value = _uiState.value.copy(
            issuerName = merchant.name,
            merchantId = merchant.id,
            merchantName = merchant.name,
            merchantSuggestions = emptyList()
        )
    }

    fun dismissFilterMerchantSuggestions() {
        _uiState.value = _uiState.value.copy(merchantSuggestions = emptyList())
    }

    private fun searchMerchantsForFilter(query: String) {
        merchantSearchJob?.cancel()
        if (query.length < 2) {
            _uiState.value = _uiState.value.copy(merchantSuggestions = emptyList(), isMerchantSearching = false)
            return
        }
        merchantSearchJob = viewModelScope.launch {
            delay(300)
            _uiState.value = _uiState.value.copy(isMerchantSearching = true)
            try {
                val results = withContext(Dispatchers.IO) {
                    expensesService.searchMerchants(query)
                }
                _uiState.value = _uiState.value.copy(merchantSuggestions = results, isMerchantSearching = false)
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(merchantSuggestions = emptyList(), isMerchantSearching = false)
            }
        }
    }

    fun setIssuerRuc(value: String) {
        _uiState.value = _uiState.value.copy(issuerRuc = value)
    }

    fun setInvoiceNumber(value: String) {
        _uiState.value = _uiState.value.copy(invoiceNumber = value)
    }

    fun setPaymentStatuses(statuses: List<String>) {
        _uiState.value = _uiState.value.copy(paymentStatuses = statuses.take(1))
    }

    fun setCategorizationStatus(status: String?) {
        _uiState.value = _uiState.value.copy(categorizationStatus = status)
    }

    fun applyInitialPaymentStatus(status: String) {
        val current = _uiState.value.paymentStatuses.firstOrNull()
        if (current == status) return
        setPaymentStatuses(listOf(status))
        applyFilters()
    }

    fun applyFilters() {
        // Validate date range (max 3 months)
        val state = _uiState.value
        val rangeError = validateDateRange(state.startDate, state.endDate)
        if (rangeError != null) {
            _uiState.value = state.copy(error = rangeError)
            return
        }
        _uiState.value = _uiState.value.copy(page = 1, expenses = emptyList(), noMore = false, error = null)
        loadExpenses(refresh = true)
    }

    private fun validateDateRange(startDate: String?, endDate: String?): String? {
        if (startDate == null && endDate == null) return null
        try {
            val start = startDate?.take(10) ?: return null
            val end = endDate?.take(10) ?: return null
            val startParts = start.split("-").map { it.toInt() }
            val endParts = end.split("-").map { it.toInt() }
            if (startParts.size != 3 || endParts.size != 3) return null
            val startMonths = startParts[0] * 12 + startParts[1]
            val endMonths = endParts[0] * 12 + endParts[1]
            val diffMonths = endMonths - startMonths
            if (diffMonths > 3 || (diffMonths == 3 && endParts[2] > startParts[2])) {
                return "El rango de fechas no puede exceder 3 meses."
            }
        } catch (_: Exception) {}
        return null
    }

    fun clearFilters() {
        _uiState.value = _uiState.value.copy(
            startDate = null,
            endDate = null,
            invoiceNumber = "",
            issuerName = "",
            issuerRuc = "",
            merchantId = null,
            merchantName = null,
            merchantSuggestions = emptyList(),
            source = null,
            paymentStatuses = emptyList(),
            categorizationStatus = null,
            searchQuery = "",
            page = 1,
            expenses = emptyList(),
            noMore = false
        )
        loadExpenses(refresh = true)
    }

    fun expenseConceptLabel(expense: Expense): String = buildExpenseConceptLabel(expense)

    fun selectExpense(expense: Expense) {
        com.teco.ventago.features.expenses.domain.ExpensesSelectionStore.selected = expense
    }
}
