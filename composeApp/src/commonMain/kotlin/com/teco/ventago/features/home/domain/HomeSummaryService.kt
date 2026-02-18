package com.teco.ventago.features.home.domain

import com.teco.ventago.core.LocalStorage
import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.features.home.data.repository.IHomeSummaryRepository
import com.teco.ventago.features.home.domain.model.HomeSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
) {

    private val summaryState = MutableStateFlow<HomeSummary?>(null)
    private val loadMutex = Mutex()
    private var currentBusinessId: Int? = null

    fun observe(): StateFlow<HomeSummary?> = summaryState.asStateFlow()

    suspend fun setBusiness(businessId: Int, refresh: Boolean = true) {
        val changedBusiness = currentBusinessId != businessId
        currentBusinessId = businessId

        if (changedBusiness) {
            summaryState.value = readCachedSummary(businessId)
        }

        if (refresh) {
            refresh(businessId)
        }
    }

    suspend fun refresh(businessIdOpt: Int? = currentBusinessId, force: Boolean = false): HomeSummary? {
        val businessId = businessIdOpt ?: return summaryState.value
        currentBusinessId = businessId

        return loadMutex.withLock {
            if (!force) {
                val freshCache = readFreshSummaryFromCache(businessId)
                if (freshCache != null) {
                    summaryState.value = freshCache
                    return@withLock freshCache
                }
            }

            runCatching { repository.getHomeSummary(businessId) }
                .onSuccess { fresh ->
                    summaryState.value = fresh
                    writeSummaryCache(businessId, fresh)
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
        summaryState.value = null
    }

    private fun readFreshSummaryFromCache(businessId: Int): HomeSummary? {
        val updatedAt = storage.long(cacheTimestampKey(businessId)) ?: return null
        val ageMs = Clock.System.now().toEpochMilliseconds() - updatedAt
        if (ageMs > CACHE_TTL_MS) return null
        return readCachedSummary(businessId)
    }

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

    private companion object {
        const val CACHE_TTL_MS = 5 * 60 * 1000L
    }
}
