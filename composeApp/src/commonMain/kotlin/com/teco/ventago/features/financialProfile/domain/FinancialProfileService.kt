package com.teco.ventago.features.financialProfile.domain

import com.teco.ventago.core.cache.ICacheService
import com.teco.ventago.core.changes.IChangesManager
import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.features.financialProfile.data.repository.IFinancialProfileRepository
import com.teco.ventago.features.financialProfile.domain.model.BusinessFinancialProfile
import com.teco.ventago.features.pos.provisioning.domain.PosDeviceProvisioningService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FinancialProfileService(
    private val cache: ICacheService,
    private val changesManager: IChangesManager,
    private val repo: IFinancialProfileRepository,
    private val loggerService: ILoggerService,
    private val appScope: CoroutineScope,
    private val posProvisioningService: PosDeviceProvisioningService,
) {

    private val state = MutableStateFlow<BusinessFinancialProfile?>(null)
    private val loadMutex = Mutex()
    private var currentBusinessId: Int? = null
    private var changesJob: Job? = null


    fun observe(): StateFlow<BusinessFinancialProfile?> = state.asStateFlow()

    fun hasLoadedProfile(): Boolean = state.value != null

    fun init() {
        appScope.launch(Dispatchers.IO) {
            // 1) try cache fast-path
            cacheGet()?.let {
                currentBusinessId = it.businessId
                state.value = it
            }
            // 3) start RTDB/Listener for invalidations
            startRealtimeSync()
        }
    }

    /** Call when you know/confirm the business (login/registration/switch). Idempotent. */
    suspend fun setBusiness(businessId: Int, refresh: Boolean = true) {
        val changedBusiness = currentBusinessId != businessId
        currentBusinessId = businessId

        // 1) Per-business cache fast-path
        if (changedBusiness || state.value == null) {
            val cachedProfile = cacheGet()
            if (cachedProfile?.businessId == businessId) {
                state.value = cachedProfile
            } else if (state.value?.businessId != businessId) {
                state.value = null
            }
        }
        // 2) Restart RT sync for this business
        startRealtimeSync()
        // 3) Refresh if requested
        if (refresh) refresh(businessId)
    }

    /** Force refresh from backend (single-flight). */
    suspend fun refresh(businessIdOpt: Int? = currentBusinessId) {
        if (businessIdOpt != null && businessIdOpt != currentBusinessId) {
            currentBusinessId = businessIdOpt
        }
        val businessId = currentBusinessId ?: return
        loadMutex.withLock {
            runCatching { repo.getFinancialProfile(businessId) }
                .onSuccess { profile ->
                    state.value = profile
                    saveCacheNow(profile, ignoreChange = true)
                    refreshPosDeviceConfig()
                }
                .onFailure {
                    loggerService.sendLog(
                        Log(
                            LogLevel.ERROR,
                            "FinancialProfileService::refresh",
                            "Error refreshing financial profile: ${it.message ?: "UNKNOWN"}"
                        )
                    )
                }
        }
    }

    private suspend fun refreshPosDeviceConfig() {
        if (!posProvisioningService.isRequired()) return
        runCatching { posProvisioningService.refreshFromKnownDevice() }
            .onFailure {
                loggerService.sendLog(
                    Log(
                        LogLevel.ERROR,
                        "FinancialProfileService::refreshPosDeviceConfig",
                        "Error refreshing POS device config: ${it.message ?: "UNKNOWN"}"
                    )
                )
            }
    }

    fun paymentsConfigured(): Boolean {
        val summary = state.value
        return summary?.let {
            val methods = it.paymentSummary.paymentMethods
            it.paymentSummary.onboardingCompleted && (
                methods.paypal.readyForPayments() ||
                    methods.yappy.linkedAccount ||
                    (methods.yappy.onsite.configured && methods.yappy.onsite.enabled) ||
                    (methods.ach.configured && methods.ach.enabled && methods.ach.isActive) ||
                    methods.card.readyForPayments() ||
                    methods.manualTransference.enabled
                )
        } ?: false
    }

    fun paymentsOnboardingCompleted(): Boolean {
        return state.value?.paymentSummary?.onboardingCompleted == true
    }

    fun paymentLinkMethodsConfigured(): Boolean {
        val summary = state.value ?: return false
        if (!summary.paymentSummary.onboardingCompleted) return false
        val methods = summary.paymentSummary.paymentMethods
        return methods.paypal.readyForPayments() ||
            methods.yappy.linkedAccount ||
            (methods.ach.configured && methods.ach.enabled && methods.ach.isActive) ||
            methods.card.readyForPayments()
    }

    fun yappyOnsiteConfigured(): Boolean {
        val onsite = state.value?.paymentSummary?.paymentMethods?.yappy?.onsite ?: return false
        return onsite.configured && onsite.enabled
    }

    fun invoicingEnabled(): Boolean {
        val summary = state.value
        return summary?.invoicingActive ?: false
    }


    /** Listen for backend changes and auto-refresh. Call once after init() */
    private fun startRealtimeSync() {
        changesJob?.cancel()
        changesJob = changesManager.financialListener()
            .onEach { value ->
                if (value != 1) {
                    refresh()
                }
            }
            .catch { e ->
                loggerService.sendLog(
                    Log(
                        LogLevel.ERROR,
                        "FinancialProfileService::startRealtimeSync",
                        "Error listening for financial profile changes: ${e.message ?: "UNKNOWN"}"
                    )
                )
            }
            .launchIn(appScope)
    }

    private suspend fun cacheGet(): BusinessFinancialProfile? =
        runCatching { cache.getCache(BusinessFinancialProfile::class) }.getOrNull()

    private suspend fun saveCacheNow(value: BusinessFinancialProfile, ignoreChange: Boolean = false) {
        runCatching { cache.saveCache(value) }
        if (!ignoreChange) {
            changesManager.financialChanged()
        }
    }

    private fun saveCache(value: BusinessFinancialProfile, ignoreChange: Boolean = false) {
        appScope.launch(Dispatchers.IO) {
            saveCacheNow(value, ignoreChange)
        }
    }

    fun clear() {
        currentBusinessId = null
        changesJob?.cancel()
        changesJob = null
        state.value = null
    }

    fun updateProfile(profile: BusinessFinancialProfile, ignoreChange: Boolean = false) {
        state.value = profile
        saveCache(profile, ignoreChange)
    }
}
