package com.teco.ventago.core.beta

import com.teco.ventago.core.beta.models.BetaFeaturesResponse
import com.teco.ventago.core.LocalStorage
import com.teco.ventago.features.auth.domain.IAuthService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class BetaService(
    private val repository: BetaRepository,
    private val authService: IAuthService,
    private val storage: LocalStorage,
    private val json: Json,
) {
    private val featuresState = MutableStateFlow<BetaFeaturesResponse?>(null)

    fun features(): StateFlow<BetaFeaturesResponse?> = featuresState.asStateFlow()

    init {
        loadCachedFeatures()
    }

    suspend fun refreshFeatures(force: Boolean = false): BetaFeaturesResponse? {
        if (!authService.isAuthenticated()) {
            return featuresState.value
        }
        val token = authService.getJwtToken()
        if (token.isNullOrBlank()) {
            return featuresState.value
        }
        if (!force && !shouldRefresh()) {
            return featuresState.value
        }
        val data = runCatching { repository.getFeatures() }.getOrNull() ?: return featuresState.value
        featuresState.value = data
        storeCachedFeatures(data)
        return data
    }

    suspend fun hasAccess(feature: BetaFeature): Boolean {
        val data = refreshFeatures()
        return data?.features?.contains(feature.key) == true
    }

    fun clear() {
        featuresState.value = null
        storage.deleteObject(CACHE_KEY_FEATURES)
        storage.deleteObject(CACHE_KEY_FETCHED_AT)
    }

    private fun shouldRefresh(): Boolean {
        val cachedAt = storage.long(CACHE_KEY_FETCHED_AT) ?: 0L
        if (cachedAt == 0L) return true
        val elapsed = System.currentTimeMillis() - cachedAt
        return elapsed >= CACHE_TTL_MS
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
            storage.set(CACHE_KEY_FETCHED_AT, System.currentTimeMillis())
        }
    }

    private companion object {
        const val CACHE_KEY_FEATURES = "beta_features_cache"
        const val CACHE_KEY_FETCHED_AT = "beta_features_cache_fetched_at"
        const val CACHE_TTL_MS = 24L * 60L * 60L * 1000L
    }
}
