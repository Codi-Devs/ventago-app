package com.teco.ventago.core.beta

import com.teco.ventago.core.beta.models.BetaFeaturesResponse
import com.teco.ventago.core.LocalStorage
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.business.domain.BusinessService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.datetime.Clock

class BetaService(
    private val repository: BetaRepository,
    private val authService: IAuthService,
    private val businessService: BusinessService,
    private val storage: LocalStorage,
    private val json: Json,
    private val appScope: CoroutineScope,
) {
    private val featuresState = MutableStateFlow<BetaFeaturesResponse?>(null)
    private val refreshMutex = Mutex()
    private var refreshJob: Job? = null

    fun features(): StateFlow<BetaFeaturesResponse?> = featuresState.asStateFlow()

    init {
        loadCachedFeatures()
        appScope.launch {
            businessService.getBusiness().collect { business ->
                val businessId = business?.businessId ?: -1
                if (businessId > 0) {
                    refreshInBackground(force = featuresState.value == null)
                }
            }
        }
    }

    /**
     * Returns cached beta features immediately when available.
     *
     * - If there is no cache, it fetches from the API and stores it.
     * - If there is cache and it is stale, it triggers a background refresh and returns the cache.
     *
     * The backend is the source of truth, but we avoid calling it more often than every [CACHE_TTL_MS].
     */
    suspend fun getFeatures(force: Boolean = false): BetaFeaturesResponse? {
        if (!authService.isAuthenticated()) {
            return featuresState.value
        }
        val token = authService.getJwtToken()
        if (token.isNullOrBlank()) {
            return featuresState.value
        }
        val businessId = businessService.business.value?.businessId ?: -1
        if (businessId <= 0) {
            return featuresState.value
        }
        val cached = featuresState.value

        if (force || cached == null) {
            return refreshNow(force = true) ?: cached
        }

        if (shouldRefresh()) {
            refreshInBackground()
        }
        return cached
    }

    suspend fun hasAccess(feature: BetaFeature): Boolean {
        return getFeatures()?.features?.contains(feature.key) == true
    }

    fun accessFlow(feature: BetaFeature) = featuresState
        .map { data -> data?.features?.contains(feature.key) == true }
        .distinctUntilChanged()

    fun refreshInBackground(force: Boolean = false) {
        if (!force && !shouldRefresh()) return
        if (refreshJob?.isActive == true) return
        refreshJob = appScope.launch {
            refreshNow(force = force)
        }
    }

    fun clear() {
        featuresState.value = null
        storage.deleteObject(CACHE_KEY_FEATURES)
        storage.deleteObject(CACHE_KEY_FETCHED_AT)
    }

    private fun shouldRefresh(): Boolean {
        val cachedAt = storage.long(CACHE_KEY_FETCHED_AT) ?: 0L
        if (cachedAt == 0L) return true
        val elapsed = nowMs() - cachedAt
        return elapsed >= CACHE_TTL_MS
    }

    private suspend fun refreshNow(force: Boolean): BetaFeaturesResponse? {
        return refreshMutex.withLock {
            if (!force && !shouldRefresh()) {
                return@withLock featuresState.value
            }
            if (!authService.isAuthenticated()) {
                return@withLock featuresState.value
            }
            val token = authService.getJwtToken()
            if (token.isNullOrBlank()) {
                return@withLock featuresState.value
            }
            val businessId = businessService.business.value?.businessId ?: -1
            if (businessId <= 0) {
                return@withLock featuresState.value
            }

            val data = runCatching { repository.getFeatures(businessId) }.getOrNull() ?: return@withLock featuresState.value
            featuresState.value = data
            storeCachedFeatures(data)
            data
        }
    }

    private fun loadCachedFeatures() {
        val cached = storage.string(CACHE_KEY_FEATURES) ?: return
        val parsed = runCatching {
            json.decodeFromString(BetaFeaturesResponse.serializer(), cached)
        }.getOrNull()
        if (parsed != null) {
            featuresState.value = parsed
        }
    }

    private fun storeCachedFeatures(data: BetaFeaturesResponse) {
        runCatching {
            val encoded = json.encodeToString(BetaFeaturesResponse.serializer(), data)
            storage.set(CACHE_KEY_FEATURES, encoded)
            storage.set(CACHE_KEY_FETCHED_AT, nowMs())
        }
    }

    private fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()

    private companion object {
        const val CACHE_KEY_FEATURES = "beta_features_cache"
        const val CACHE_KEY_FETCHED_AT = "beta_features_cache_fetched_at"
        const val CACHE_TTL_MS = 12L * 60L * 60L * 1000L
    }
}
