package com.teco.ventago.features.quotes.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.PdfSharer
import com.teco.ventago.core.authz.ActionKey
import com.teco.ventago.core.authz.AuthzEvaluator
import com.teco.ventago.core.beta.BetaFeature
import com.teco.ventago.core.beta.BetaService
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.quotes.domain.QuotesService
import com.teco.ventago.features.quotes.domain.models.requests.CancelQuoteRequest
import com.teco.ventago.features.quotes.domain.models.requests.GetQuoteRequest
import com.teco.ventago.features.quotes.domain.models.requests.SendQuoteEmailRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuoteDetailsViewModel(
    private val quotesService: QuotesService,
    private val pdfSharer: PdfSharer,
    private val authService: IAuthService,
    private val betaService: BetaService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuoteDetailsState())
    val uiState: StateFlow<QuoteDetailsState> = _uiState.asStateFlow()

    init {
        authService.getUser()
            .combine(betaService.features()) { user, betaResponse ->
                val betaSnapshot = betaResponse?.features.orEmpty()
                    .mapNotNull(BetaFeature::fromKey)
                    .toSet()
                Triple(
                    AuthzEvaluator.canAction(ActionKey.QUOTES_UPDATE, user, betaSnapshot),
                    AuthzEvaluator.canAction(ActionKey.ORDERS_OPEN_CREATE, user, betaSnapshot),
                    AuthzEvaluator.canAction(ActionKey.QUOTES_UPDATE, user, betaSnapshot)
                )
            }
            .onEach { (canModifyQuote, canCreateOrderFromQuote, canCancelQuote) ->
                _uiState.value = _uiState.value.copy(
                    canModifyQuote = canModifyQuote,
                    canCreateOrderFromQuote = canCreateOrderFromQuote,
                    canCancelQuote = canCancelQuote
                )
            }
            .launchIn(viewModelScope)
    }

    fun loadQuote(quoteId: Long? = null, quoteNumber: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val quote = withContext(Dispatchers.IO) {
                    val selected = com.teco.ventago.features.quotes.domain.QuoteSelectionStore.selected
                    val idToLoad = quoteId ?: selected?.id
                    val numberToLoad = quoteNumber ?: selected?.displayNumber ?: selected?.quoteNumber
                    quotesService.getQuote(GetQuoteRequest(quoteId = idToLoad, quoteNumber = numberToLoad))
                }
                _uiState.value = _uiState.value.copy(quote = quote, isLoading = false, error = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun cancel(reason: String) {
        if (!_uiState.value.canCancelQuote) {
            _uiState.value = _uiState.value.copy(error = "No autorizado para cancelar cotizaciones.")
            return
        }
        val quoteId = _uiState.value.quote?.id ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCancelling = true, error = null)
            try {
                withContext(Dispatchers.IO) {
                    quotesService.cancelQuote(CancelQuoteRequest(quoteId = quoteId, reason = reason))
                }
                loadQuote(quoteId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            } finally {
                _uiState.value = _uiState.value.copy(isCancelling = false)
            }
        }
    }

    fun sendEmail(email: String) {
        val quoteId = _uiState.value.quote?.id ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSendingEmail = true, error = null)
            try {
                withContext(Dispatchers.IO) {
                    quotesService.sendQuoteEmail(SendQuoteEmailRequest(quoteId = quoteId, recipientEmail = email))
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            } finally {
                _uiState.value = _uiState.value.copy(isSendingEmail = false)
            }
        }
    }

    fun downloadPdf() {
        val quoteId = _uiState.value.quote?.id ?: return
        if (_uiState.value.isDownloadingPdf) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDownloadingPdf = true, error = null)
            try {
                val pdfB64 = withContext(Dispatchers.IO) { quotesService.getQuotePdf(quoteId) }
                val pdfBytes = kotlin.io.encoding.Base64.decode(pdfB64)
                val filename = "quote_${quoteId}.pdf"
                pdfSharer.openPdf(filename, pdfBytes)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            } finally {
                _uiState.value = _uiState.value.copy(isDownloadingPdf = false)
            }
        }
    }
}
