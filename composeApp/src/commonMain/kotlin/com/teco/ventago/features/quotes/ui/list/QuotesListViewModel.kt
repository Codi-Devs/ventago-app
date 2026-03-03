package com.teco.ventago.features.quotes.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teco.ventago.features.quotes.domain.QuotesService
import com.teco.ventago.features.quotes.domain.models.Quote
import com.teco.ventago.features.quotes.domain.models.requests.ListQuotesRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.LinkedHashMap

class QuotesListViewModel(
    private val quotesService: QuotesService
) : ViewModel() {

    private companion object {
        const val INITIAL_PAGE = 1
        const val PAGE_SIZE = 10
    }

    private val _uiState = MutableStateFlow(QuotesListState())
    val uiState: StateFlow<QuotesListState> = _uiState.asStateFlow()

    init {
        loadQuotes(refresh = true)
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
                    quotesService.listQuotes(
                        ListQuotesRequest(
                            page = currentPage,
                            pageSize = PAGE_SIZE,
                            customerName = filters.customerName.ifBlank { null },
                            customerRuc = filters.customerRuc.ifBlank { null },
                            quoteNumber = filters.quoteNumber.ifBlank { null },
                            status = filters.status
                        )
                    )
                }
                val newQuotes = mergeQuotes(
                    existing = if (refresh) emptyList() else _uiState.value.quotes,
                    incoming = response.items
                )
                val noMore = response.items.isEmpty() || (response.size != null && response.items.size < (response.size ?: PAGE_SIZE))
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
        _uiState.value = _uiState.value.copy(customerName = value)
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

    fun applyFilters() {
        _uiState.value = _uiState.value.copy(page = INITIAL_PAGE, quotes = emptyList(), noMore = false)
        loadQuotes(refresh = true)
    }

    fun clearFilters() {
        _uiState.value = _uiState.value.copy(
            customerName = "",
            customerRuc = "",
            quoteNumber = "",
            status = null,
            page = INITIAL_PAGE,
            quotes = emptyList(),
            noMore = false
        )
        loadQuotes(refresh = true)
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
