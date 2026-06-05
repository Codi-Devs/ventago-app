package com.teco.ventago.features.invoicing.domain

import com.teco.ventago.core.LocalStorage
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.business.domain.BusinessService
import com.teco.ventago.features.invoicing.data.repository.IInvoicingSettingsRepository
import com.teco.ventago.features.invoicing.domain.models.BottomNoteSettings
import com.teco.ventago.features.invoicing.domain.models.BottomNoteSettingsRequest
import com.teco.ventago.features.invoicing.domain.models.BottomNoteSettingsState
import com.teco.ventago.features.invoicing.domain.models.InvoicingSettings
import com.teco.ventago.features.invoicing.domain.models.InvoicingSettingsState
import com.teco.ventago.features.invoicing.domain.models.IncludeAddressOnInvoiceRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface InvoicingSettingsStore {
    fun set(key: String, value: String): Boolean
    fun string(key: String): String?
    fun deleteObject(key: String): Boolean
}

class LocalStorageInvoicingSettingsStore(
    private val storage: LocalStorage,
) : InvoicingSettingsStore {
    override fun set(key: String, value: String): Boolean = storage.set(key, value)
    override fun string(key: String): String? = storage.string(key)
    override fun deleteObject(key: String): Boolean = storage.deleteObject(key)
}

class InvoicingSettingsService(
    private val repository: IInvoicingSettingsRepository,
    private val businessService: BusinessService,
    private val authService: IAuthService,
    private val store: InvoicingSettingsStore,
    private val json: Json,
    appScope: CoroutineScope,
) {
    private val invoicingSettingsState = MutableStateFlow(InvoicingSettingsState())
    private val bottomNoteState = MutableStateFlow(BottomNoteSettingsState())
    private var loadedBusinessId: Int? = null

    init {
        businessService.getBusiness()
            .onEach { business ->
                val businessId = business?.businessId
                if (businessId == null) {
                    loadedBusinessId = null
                    invoicingSettingsState.value = InvoicingSettingsState()
                    bottomNoteState.value = BottomNoteSettingsState()
                } else if (businessId != loadedBusinessId) {
                    loadCachedInvoicingSettings(businessId)
                    loadCachedBottomNoteSettings(businessId)
                }
            }
            .launchIn(appScope)
    }

    fun invoicingSettings(): StateFlow<InvoicingSettingsState> = invoicingSettingsState.asStateFlow()

    fun bottomNoteSettings(): StateFlow<BottomNoteSettingsState> = bottomNoteState.asStateFlow()

    fun loadCachedInvoicingSettingsForCurrentBusiness() {
        businessId()?.let(::loadCachedInvoicingSettings)
    }

    fun loadCachedBottomNoteSettingsForCurrentBusiness() {
        businessId()?.let(::loadCachedBottomNoteSettings)
    }

    suspend fun refreshInvoicingSettings(): InvoicingSettings {
        if (!authService.isAuthenticated() || authService.getJwtToken().isNullOrBlank()) {
            return invoicingSettingsState.value.settings
        }
        val businessId = businessId() ?: return invoicingSettingsState.value.settings
        loadCachedInvoicingSettings(businessId)
        return runCatching { repository.getInvoicingSettings(businessId) }
            .onSuccess { settings ->
                invoicingSettingsState.value = InvoicingSettingsState(settings = settings, refreshFailed = false)
                storeCachedInvoicingSettings(businessId, settings)
            }
            .onFailure {
                invoicingSettingsState.value = invoicingSettingsState.value.copy(refreshFailed = true)
            }
            .getOrDefault(invoicingSettingsState.value.settings)
    }

    suspend fun updateIncludeAddressOnInvoice(include: Boolean): Boolean {
        if (!authService.isAuthenticated()) return false
        val businessId = businessId() ?: return false
        val updated = repository.updateIncludeAddressOnInvoice(
            businessId,
            IncludeAddressOnInvoiceRequest(includeAddressOnInvoice = include),
        )
        if (updated) {
            val settings = invoicingSettingsState.value.settings.copy(includeAddressOnInvoice = include)
            invoicingSettingsState.value = InvoicingSettingsState(settings = settings, refreshFailed = false)
            storeCachedInvoicingSettings(businessId, settings)
        }
        return updated
    }

    suspend fun refreshBottomNoteSettings(): BottomNoteSettings? {
        if (!authService.isAuthenticated() || authService.getJwtToken().isNullOrBlank()) {
            return bottomNoteState.value.settings
        }
        val businessId = businessId() ?: return bottomNoteState.value.settings
        loadCachedBottomNoteSettings(businessId)
        return runCatching { repository.getBottomNoteSettings(businessId) }
            .onSuccess { settings ->
                bottomNoteState.value = BottomNoteSettingsState(settings = settings, refreshFailed = false)
                if (settings == null) {
                    clearCachedBottomNoteSettings(businessId)
                } else {
                    storeCachedBottomNoteSettings(businessId, settings)
                }
            }
            .onFailure {
                bottomNoteState.value = bottomNoteState.value.copy(refreshFailed = true)
            }
            .getOrNull()
    }

    suspend fun saveBottomNoteSettings(request: BottomNoteSettingsRequest): BottomNoteSettings? {
        if (!authService.isAuthenticated()) return null
        val businessId = businessId() ?: return null
        val hasExistingSettings = bottomNoteState.value.settings != null
        val settings = if (hasExistingSettings) {
            repository.updateBottomNoteSettings(businessId, request)
        } else {
            repository.createBottomNoteSettings(businessId, request)
        }
        bottomNoteState.value = BottomNoteSettingsState(settings = settings, refreshFailed = false)
        storeCachedBottomNoteSettings(businessId, settings)
        return settings
    }

    suspend fun updateBottomNoteSettings(request: BottomNoteSettingsRequest): BottomNoteSettings? {
        if (!authService.isAuthenticated()) return null
        val businessId = businessId() ?: return null
        val settings = repository.updateBottomNoteSettings(businessId, request)
        bottomNoteState.value = BottomNoteSettingsState(settings = settings, refreshFailed = false)
        storeCachedBottomNoteSettings(businessId, settings)
        return settings
    }

    suspend fun deleteBottomNoteSettings(): Boolean {
        if (!authService.isAuthenticated()) return false
        val businessId = businessId() ?: return false
        val deleted = repository.deleteBottomNoteSettings(businessId)
        if (deleted) {
            bottomNoteState.value = BottomNoteSettingsState()
            clearCachedBottomNoteSettings(businessId)
        }
        return deleted
    }

    fun clearBottomNoteSettings() {
        loadedBusinessId?.let(::clearCachedBottomNoteSettings)
        loadedBusinessId = null
        bottomNoteState.value = BottomNoteSettingsState()
    }

    fun clearInvoicingSettings() {
        loadedBusinessId?.let(::clearCachedInvoicingSettings)
        invoicingSettingsState.value = InvoicingSettingsState()
    }

    private fun businessId(): Int? = businessService.business.value?.businessId

    private fun loadCachedBottomNoteSettings(businessId: Int) {
        loadedBusinessId = businessId
        val cached = store.string(cacheKey(businessId))
        val parsed = cached?.let {
            runCatching {
                json.decodeFromString(BottomNoteSettings.serializer(), it)
            }.getOrNull()
        }
        bottomNoteState.value = BottomNoteSettingsState(settings = parsed, refreshFailed = false)
    }

    private fun loadCachedInvoicingSettings(businessId: Int) {
        loadedBusinessId = businessId
        val cached = store.string(invoicingSettingsCacheKey(businessId))
        val parsed = cached?.let {
            runCatching {
                json.decodeFromString(InvoicingSettings.serializer(), it)
            }.getOrNull()
        }
        invoicingSettingsState.value = InvoicingSettingsState(
            settings = parsed ?: InvoicingSettings(),
            refreshFailed = false,
        )
    }

    private fun storeCachedBottomNoteSettings(businessId: Int, settings: BottomNoteSettings) {
        runCatching {
            store.set(
                cacheKey(businessId),
                json.encodeToString(BottomNoteSettings.serializer(), settings),
            )
        }
    }

    private fun storeCachedInvoicingSettings(businessId: Int, settings: InvoicingSettings) {
        runCatching {
            store.set(
                invoicingSettingsCacheKey(businessId),
                json.encodeToString(InvoicingSettings.serializer(), settings),
            )
        }
    }

    private fun clearCachedBottomNoteSettings(businessId: Int) {
        store.deleteObject(cacheKey(businessId))
    }

    private fun clearCachedInvoicingSettings(businessId: Int) {
        store.deleteObject(invoicingSettingsCacheKey(businessId))
    }

    private fun cacheKey(businessId: Int): String = "$CACHE_KEY_PREFIX.$businessId"

    private fun invoicingSettingsCacheKey(businessId: Int): String = "$SETTINGS_CACHE_KEY_PREFIX.$businessId"

    private companion object {
        const val CACHE_KEY_PREFIX = "invoicing_bottom_note_settings_cache"
        const val SETTINGS_CACHE_KEY_PREFIX = "invoicing_settings_cache"
    }
}
