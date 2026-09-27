package com.teco.ventago.features.quotes.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teco.ventago.features.branches.domain.BranchService
import com.teco.ventago.features.business.domain.BusinessService
import com.teco.ventago.features.customers.domain.CustomerService
import com.teco.ventago.features.customers.domain.models.CustomerListItem
import com.teco.ventago.features.quotes.domain.QuotesService
import com.teco.ventago.features.quotes.domain.models.Quote
import com.teco.ventago.features.quotes.domain.models.requests.ListQuotesRequest
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
import kotlin.collections.LinkedHashMap

class QuotesListViewModel(
    private val quotesService: QuotesService,
    private val branchService: BranchService,
    private val customerService: CustomerService,
    private val businessService: BusinessService,
) : ViewModel() {

    private companion object {
        const val INITIAL_PAGE = 1
        const val PAGE_SIZE = 10
        const val CUSTOMER_SEARCH_LIMIT = 4
        const val MIN_CUSTOMER_SEARCH_LENGTH = 2
        const val CUSTOMER_SEARCH_DEBOUNCE_MS = 300L
    }

    private val _uiState = MutableStateFlow(QuotesListState())
    val uiState: StateFlow<QuotesListState> = _uiState.asStateFlow()
    private var customerSearchJob: Job? = null

    init {
        loadQuotes(refresh = true)
        branchService.observe()
            .onEach { branches ->
                _uiState.value = _uiState.value.copy(branches = branches.toList())
            }
            .launchIn(viewModelScope)
        quotesService.quoteSettings()
            .onEach { settings ->
                _uiState.value = _uiState.value.copy(
                    quotePrefix = settings?.quotePrefix?.takeIf { it.isNotBlank() } ?: "COT"
                )
            }
            .launchIn(viewModelScope)
        viewModelScope.launch {
            quotesService.refreshQuoteSettings()
        }
    }

    fun loadQuotes(refresh: Boolean = false) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState.isLoading || currentState.refreshing || (!refresh && currentState.noMore)) {
                return@launch
            }

            val currentPage = if (refresh) INITIAL_PAGE else currentState.page
            _uiState.value = currentState.copy(
                isLoading = !refresh && currentState.quotes.isEmpty(),
                refreshing = refresh
            )
            try {
                val filters = _uiState.value
                val response = withContext(Dispatchers.IO) {
                    quotesService.listQuotes(filters.toListQuotesRequest(currentPage, PAGE_SIZE))
                }
                val newQuotes = mergeQuotes(
                    existing = if (refresh) emptyList() else _uiState.value.quotes,
                    incoming = response.items
                )
                val noMore = response.items.isEmpty() ||
                    (response.size != null && response.items.size < response.size)
                _uiState.value = _uiState.value.copy(
                    quotes = newQuotes,
                    page = currentPage + 1,
                    isLoading = false,
                    refreshing = false,
                    noMore = noMore,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    refreshing = false,
                    error = e.message
                )
            }
        }
    }

    fun selectQuote(quote: Quote) {
        // Could store selection in state handle if needed
    }

    fun setCustomerName(value: String) {
        customerSearchJob?.cancel()
        val query = value.trim()
        _uiState.value = _uiState.value.copy(
            customerName = value,
            customerRuc = "",
            selectedCustomerId = null,
            customerSearchResults = if (query.length < MIN_CUSTOMER_SEARCH_LENGTH) {
                emptyList()
            } else {
                _uiState.value.customerSearchResults
            },
            isSearchingCustomers = query.length >= MIN_CUSTOMER_SEARCH_LENGTH,
        )

        val businessId = businessService.business.value?.businessId
        if (query.length < MIN_CUSTOMER_SEARCH_LENGTH || businessId == null || businessId <= 0) {
            _uiState.value = _uiState.value.copy(isSearchingCustomers = false)
            return
        }

        customerSearchJob = viewModelScope.launch {
            delay(CUSTOMER_SEARCH_DEBOUNCE_MS)
            try {
                val customers = withContext(Dispatchers.IO) {
                    customerService.searchCustomersByName(
                        businessId = businessId,
                        name = query,
                        limit = CUSTOMER_SEARCH_LIMIT,
                    )
                }
                if (_uiState.value.customerName == value) {
                    _uiState.value = _uiState.value.copy(
                        customerSearchResults = customers.take(CUSTOMER_SEARCH_LIMIT),
                        isSearchingCustomers = false,
                    )
                }
            } catch (_: Exception) {
                if (_uiState.value.customerName == value) {
                    _uiState.value = _uiState.value.copy(
                        customerSearchResults = emptyList(),
                        isSearchingCustomers = false,
                    )
                }
            }
        }
    }

    fun selectCustomer(customer: CustomerListItem) {
        customerSearchJob?.cancel()
        _uiState.value = _uiState.value.copy(
            customerName = customer.name,
            customerRuc = customer.ruc.orEmpty(),
            selectedCustomerId = customer.id,
            customerSearchResults = emptyList(),
            isSearchingCustomers = false,
            quotes = emptyList(),
            noMore = false,
        )
        loadQuotes(refresh = true)
    }

    fun clearCustomerSearch() {
        customerSearchJob?.cancel()
        _uiState.value = _uiState.value.copy(
            customerName = "",
            customerRuc = "",
            selectedCustomerId = null,
            customerSearchResults = emptyList(),
            isSearchingCustomers = false,
            quotes = emptyList(),
            noMore = false,
        )
        loadQuotes(refresh = true)
    }

    fun setCustomerRuc(value: String) {
        _uiState.value = _uiState.value.copy(customerRuc = value)
    }

    fun setQuoteNumber(value: String) {
        _uiState.value = _uiState.value.copy(quoteNumber = value)
    }

    fun setStatus(value: Int?) {
        _uiState.value = _uiState.value.copy(status = value)
    }

    fun showFiltersSheet(show: Boolean) {
        _uiState.value = _uiState.value.copy(showFiltersSheet = show)
    }

    fun showQuoteSearchSheet(show: Boolean) {
        _uiState.value = _uiState.value.copy(showQuoteSearchSheet = show)
    }

    fun activeFilterCount(): Int {
        val state = _uiState.value
        return listOf(
            state.quoteNumber.isNotBlank(),
            state.status != null,
        ).count { it }
    }

    fun applyFilters() {
        _uiState.value = _uiState.value.copy(page = INITIAL_PAGE, quotes = emptyList(), noMore = false)
        loadQuotes(refresh = true)
    }

    fun clearFilters() {
        _uiState.value = _uiState.value.copy(
            quoteNumber = "",
            status = null,
            page = INITIAL_PAGE,
            quotes = emptyList(),
            noMore = false
        )
        loadQuotes(refresh = true)
    }

    override fun onCleared() {
        customerSearchJob?.cancel()
        super.onCleared()
    }

    private fun mergeQuotes(existing: List<Quote>, incoming: List<Quote>): List<Quote> {
        val mergedQuotes = LinkedHashMap<String, Quote>()
        (existing + incoming).forEachIndexed { index, quote ->
            mergedQuotes[quoteDedupKey(quote, index)] = quote
        }
        return mergedQuotes.values.toList()
    }

    private fun quoteDedupKey(quote: Quote, index: Int): String {
        return when {
            quote.id != null -> "id:${quote.id}"
            !quote.quoteNumber.isNullOrBlank() -> "quote_number:${quote.quoteNumber}"
            !quote.displayNumber.isNullOrBlank() -> "display_number:${quote.displayNumber}"
            else -> "fallback:$index:${quote.hashCode()}"
        }
    }
}

internal fun QuotesListState.toListQuotesRequest(page: Int, pageSize: Int): ListQuotesRequest =
    ListQuotesRequest(
        page = page,
        pageSize = pageSize,
        customerName = customerName.ifBlank { null },
        customerRuc = customerRuc.ifBlank { null },
        quoteNumber = quoteNumber.ifBlank { null },
        status = status,
    )
