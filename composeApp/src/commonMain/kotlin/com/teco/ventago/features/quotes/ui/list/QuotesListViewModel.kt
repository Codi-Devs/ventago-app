package com.teco.ventago.features.quotes.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teco.ventago.features.quotes.domain.QuotesService
import com.teco.ventago.features.quotes.domain.models.Quote
import com.teco.ventago.features.quotes.domain.models.requests.ListQuotesRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuotesListViewModel(
    private val quotesService: QuotesService
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuotesListState())
    val uiState: StateFlow<QuotesListState> = _uiState.asStateFlow()

    init {
        loadQuotes(refresh = true)
    }

    fun loadQuotes(refresh: Boolean = false) {
        viewModelScope.launch {
            val currentPage = if (refresh) 0 else _uiState.value.page
            _uiState.value = _uiState.value.copy(
                isLoading = !refresh && _uiState.value.quotes.isEmpty(),
                refreshing = refresh
            )
            try {
                val filters = _uiState.value
                val response = withContext(Dispatchers.IO) {
                    quotesService.listQuotes(
                        ListQuotesRequest(
                            page = currentPage,
                            pageSize = 10,
                            customerName = filters.customerName.ifBlank { null },
                            customerRuc = filters.customerRuc.ifBlank { null },
                            quoteNumber = filters.quoteNumber.ifBlank { null },
                            status = filters.status
                        )
                    )
                }
                val newQuotes = if (refresh) response.items else _uiState.value.quotes + response.items
                val noMore = response.items.isEmpty() || (response.size != null && response.items.size < (response.size ?: 10))
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
        _uiState.value = _uiState.value.copy(page = 0, quotes = emptyList(), noMore = false)
        loadQuotes(refresh = true)
    }

    fun clearFilters() {
        _uiState.value = _uiState.value.copy(
            customerName = "",
            customerRuc = "",
            quoteNumber = "",
            status = null,
            page = 0,
            quotes = emptyList(),
            noMore = false
        )
        loadQuotes(refresh = true)
    }
}
