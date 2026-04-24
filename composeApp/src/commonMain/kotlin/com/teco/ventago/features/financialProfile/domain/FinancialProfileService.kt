package com.teco.ventago.features.financialProfile.domain

import com.teco.ventago.core.cache.ICacheService
import com.teco.ventago.core.changes.IChangesManager
import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.features.financialProfile.data.repository.IFinancialProfileRepository
import com.teco.ventago.features.financialProfile.domain.model.BusinessFinancialProfile
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
    private val appScope: CoroutineScope
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
        if (currentBusinessId == businessId && state.value != null) return
        currentBusinessId = businessId

        // 1) Per-business cache fast-path
        cacheGet()?.let { state.value = it }
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
                    saveCache(profile, ignoreChange = true)
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

    fun paymentsConfigured(): Boolean {
        val summary = state.value
        return summary?.let {
            it.paymentSummary.onboardingCompleted && (it.paymentSummary.paymentMethods.paypal.linkedAccount || it.paymentSummary.paymentMethods.yappy.linkedAccount
                    || (it.paymentSummary.paymentMethods.ach.configured && it.paymentSummary.paymentMethods.ach.enabled)
                    || it.paymentSummary.paymentMethods.manualTransference.enabled)
        } ?: false
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

    private fun saveCache(value: BusinessFinancialProfile, ignoreChange: Boolean = false) {
        appScope.launch(Dispatchers.IO) {
            runCatching { cache.saveCache(value) }
            if (!ignoreChange) {
                changesManager.financialChanged()
            }
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
