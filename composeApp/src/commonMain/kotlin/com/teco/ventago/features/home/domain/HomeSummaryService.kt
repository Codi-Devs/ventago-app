package com.teco.ventago.features.home.domain

import com.teco.ventago.core.LocalStorage
import com.teco.ventago.core.changes.IChangesManager
import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.features.home.data.repository.IHomeSummaryRepository
import com.teco.ventago.features.home.domain.model.HomeSummary
import kotlinx.coroutines.CoroutineScope
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.datetime.Clock

class HomeSummaryService(
    private val repository: IHomeSummaryRepository,
    private val storage: LocalStorage,
    private val logger: ILoggerService,
    private val json: Json,
    private val changesManager: IChangesManager,
    private val appScope: CoroutineScope,
) {

    private val summaryState = MutableStateFlow<HomeSummary?>(null)
    private val loadMutex = Mutex()
    private var currentBusinessId: Int? = null
    private var pendingInvalidate = false
    private var changesJob: Job? = null

    init {
        startRealtimeSync()
    }

    fun observe(): StateFlow<HomeSummary?> = summaryState.asStateFlow()

    suspend fun setBusiness(businessId: Int, refresh: Boolean = true) {
        val changedBusiness = currentBusinessId != businessId
        currentBusinessId = businessId

        if (changedBusiness) {
            summaryState.value = readCachedSummary(businessId)
        }

        if (refresh) {
            val force = pendingInvalidate
            pendingInvalidate = false
            refresh(businessId, force = force)
        }
    }

    suspend fun refresh(businessIdOpt: Int? = currentBusinessId, force: Boolean = false): HomeSummary? {
        val businessId = businessIdOpt ?: return summaryState.value
        currentBusinessId = businessId

        return loadMutex.withLock {
            if (!HomeSummaryCachePolicy.shouldCallApi(force, hasCachedPayload(businessId))) {
                val cached = readCachedSummary(businessId)
                if (cached != null) {
                    summaryState.value = cached
                    return@withLock cached
                }
            }

            runCatching { repository.getHomeSummary(businessId) }
                .onSuccess { fresh ->
                    summaryState.value = fresh
                    writeSummaryCache(businessId, fresh)
                    pendingInvalidate = false
                }
                .onFailure { error ->
                    logger.sendLog(
                        Log(
                            LogLevel.ERROR,
                            "HomeSummaryService::refresh",
                            "Error refreshing home summary. businessId: $businessId, error: ${error.message ?: "UNKNOWN"}"
                        )
                    )
                }
                .getOrNull() ?: summaryState.value
        }
    }

    fun clear() {
        currentBusinessId = null
        pendingInvalidate = false
        changesJob?.cancel()
        changesJob = null
        summaryState.value = null
    }

    private fun startRealtimeSync() {
        changesJob?.cancel()
        changesJob = changesManager.homeSummaryListener()
            .onEach { value ->
                if (value == 1) return@onEach
                val businessId = currentBusinessId
                if (businessId == null) {
                    pendingInvalidate = true
                    return@onEach
                }
                appScope.launch {
                    refresh(businessId, force = true)
                }
            }
            .catch { error ->
                logger.sendLog(
                    Log(
                        LogLevel.ERROR,
                        "HomeSummaryService::startRealtimeSync",
                        "Error listening for home summary cache changes: ${error.message ?: "UNKNOWN"}"
                    )
                )
            }
            .launchIn(appScope)
    }

    private fun hasCachedPayload(businessId: Int): Boolean =
        !storage.string(cachePayloadKey(businessId)).isNullOrBlank()

    private fun readCachedSummary(businessId: Int): HomeSummary? {
        val payload = storage.string(cachePayloadKey(businessId)) ?: return null
        return runCatching {
            json.decodeFromString(HomeSummary.serializer(), payload)
        }.getOrNull()
    }

    private fun writeSummaryCache(businessId: Int, summary: HomeSummary) {
        runCatching {
            storage.set(cachePayloadKey(businessId), json.encodeToString(HomeSummary.serializer(), summary))
            storage.set(cacheTimestampKey(businessId), Clock.System.now().toEpochMilliseconds())
        }
    }

    private fun cachePayloadKey(businessId: Int): String = "home_summary_cache_payload_$businessId"

    private fun cacheTimestampKey(businessId: Int): String = "home_summary_cache_ts_$businessId"
}

object HomeSummaryCachePolicy {
    fun shouldCallApi(force: Boolean, hasCachedPayload: Boolean): Boolean {
        return force || !hasCachedPayload
    }
}
