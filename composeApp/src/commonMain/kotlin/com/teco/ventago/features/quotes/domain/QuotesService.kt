package com.teco.ventago.features.quotes.domain

import com.teco.ventago.core.LocalStorage
import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.business.domain.BusinessService
import com.teco.ventago.features.quotes.data.repository.IQuotesRepository
import com.teco.ventago.features.quotes.domain.models.PagedQuotes
import com.teco.ventago.features.quotes.domain.models.Quote
import com.teco.ventago.features.quotes.domain.models.QuoteSettings
import com.teco.ventago.features.quotes.domain.models.requests.CancelQuoteRequest
import com.teco.ventago.features.quotes.domain.models.requests.CreateQuoteRequest
import com.teco.ventago.features.quotes.domain.models.requests.GetQuoteRequest
import com.teco.ventago.features.quotes.domain.models.requests.ListQuotesRequest
import com.teco.ventago.features.quotes.domain.models.requests.SendQuoteEmailRequest
import com.teco.ventago.features.quotes.domain.models.requests.UpdateQuoteRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class QuotesService(
    private val repository: IQuotesRepository,
    private val businessService: BusinessService,
    private val logger: ILoggerService,
    private val authService: IAuthService,
    private val storage: LocalStorage,
    private val json: Json,
) {

    private val settingsState = MutableStateFlow<QuoteSettings?>(null)

    private fun businessId(): Int? = businessService.business.value?.businessId

    init {
        loadCachedSettings()
    }

    fun quoteSettings(): StateFlow<QuoteSettings?> = settingsState.asStateFlow()

    suspend fun listQuotes(request: ListQuotesRequest): PagedQuotes {
        val businessId = businessId() ?: throw IllegalStateException("No business selected")
        return repository.listQuotes(businessId, request)
    }

    suspend fun getQuote(request: GetQuoteRequest): Quote {
        val businessId = businessId() ?: throw IllegalStateException("No business selected")
        return repository.getQuote(businessId, request)
    }

    suspend fun getQuotePdf(quoteId: Long): String {
        val businessId = businessId() ?: throw IllegalStateException("No business selected")
        return repository.getQuotePdf(businessId, quoteId)
    }

    suspend fun sendQuoteEmail(request: SendQuoteEmailRequest): Boolean {
        val businessId = businessId() ?: throw IllegalStateException("No business selected")
        return repository.sendQuoteEmail(businessId, request)
    }

    suspend fun cancelQuote(request: CancelQuoteRequest): Boolean {
        val businessId = businessId() ?: throw IllegalStateException("No business selected")
        return repository.cancelQuote(businessId, request)
    }

    suspend fun createQuote(request: CreateQuoteRequest): Quote {
        val businessId = businessId() ?: throw IllegalStateException("No business selected")
        return repository.createQuote(businessId, request)
    }

    suspend fun updateQuote(request: UpdateQuoteRequest): Quote {
        val businessId = businessId() ?: throw IllegalStateException("No business selected")
        return repository.updateQuote(businessId, request)
    }

    suspend fun refreshQuoteSettings(): QuoteSettings? {
        if (!authService.isAuthenticated()) {
            return settingsState.value
        }
        if (authService.getJwtToken().isNullOrBlank()) {
            return settingsState.value
        }
        val businessId = businessId() ?: return settingsState.value
        val settings = runCatching { repository.getQuoteSettings(businessId) }
            .getOrNull() ?: return settingsState.value
        settingsState.value = settings
        storeCachedSettings(settings)
        return settings
    }

    suspend fun updateQuoteSettings(settings: QuoteSettings): Boolean {
        if (!authService.isAuthenticated()) {
            return false
        }
        val businessId = businessId() ?: return false
        val updated = runCatching { repository.updateQuoteSettings(businessId, settings) }
            .getOrDefault(false)
        if (updated) {
            settingsState.value = settings
            storeCachedSettings(settings)
        }
        return updated
    }

    fun clearSettings() {
        settingsState.value = null
        storage.deleteObject(CACHE_KEY_SETTINGS)
    }

    private fun loadCachedSettings() {
        val cached = storage.string(CACHE_KEY_SETTINGS) ?: return
        val parsed = runCatching {
            json.decodeFromString(QuoteSettings.serializer(), cached)
        }.getOrNull()
        if (parsed != null) {
            settingsState.value = parsed
        }
    }

    private fun storeCachedSettings(settings: QuoteSettings) {
        runCatching {
            val encoded = json.encodeToString(QuoteSettings.serializer(), settings)
            storage.set(CACHE_KEY_SETTINGS, encoded)
        }
    }

    private companion object {
        const val CACHE_KEY_SETTINGS = "quote_settings_cache"
    }
}
