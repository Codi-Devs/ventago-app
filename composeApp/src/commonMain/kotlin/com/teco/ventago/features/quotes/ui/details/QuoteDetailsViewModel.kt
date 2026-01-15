package com.teco.ventago.features.quotes.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.PdfSharer
import com.teco.ventago.features.quotes.domain.QuotesService
import com.teco.ventago.features.quotes.domain.models.requests.CancelQuoteRequest
import com.teco.ventago.features.quotes.domain.models.requests.GetQuoteRequest
import com.teco.ventago.features.quotes.domain.models.requests.SendQuoteEmailRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuoteDetailsViewModel(
    private val quotesService: QuotesService,
    private val pdfSharer: PdfSharer
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuoteDetailsState())
    val uiState: StateFlow<QuoteDetailsState> = _uiState.asStateFlow()

    fun loadQuote(quoteId: Long? = null, quoteNumber: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val quote = withContext(Dispatchers.IO) {
                    val selected = com.teco.ventago.features.quotes.domain.QuoteSelectionStore.selected
                    val idToLoad = quoteId ?: selected?.id
                    val numberToLoad = quoteNumber ?: selected?.quoteNumber
                    quotesService.getQuote(GetQuoteRequest(quoteId = idToLoad, quoteNumber = numberToLoad))
                }
                _uiState.value = _uiState.value.copy(quote = quote, isLoading = false, error = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun cancel(reason: String) {
        val quoteId = _uiState.value.quote?.id ?: return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    quotesService.cancelQuote(CancelQuoteRequest(quoteId = quoteId, reason = reason))
                }
                loadQuote(quoteId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun sendEmail(email: String) {
        val quoteId = _uiState.value.quote?.id ?: return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    quotesService.sendQuoteEmail(SendQuoteEmailRequest(quoteId = quoteId, recipientEmail = email))
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun downloadPdf() {
        val quoteId = _uiState.value.quote?.id ?: return
        viewModelScope.launch {
            try {
                val pdfB64 = withContext(Dispatchers.IO) { quotesService.getQuotePdf(quoteId) }
                val pdfBytes = kotlin.io.encoding.Base64.decode(pdfB64)
                val filename = "quote_${quoteId}.pdf"
                pdfSharer.openPdf(filename, pdfBytes)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}
