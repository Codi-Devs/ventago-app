package com.teco.ventago.core.version

import com.teco.ventago.core.LocalStorage
import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.remoteconfig.remoteConfig
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.withTimeout

class FirebaseVersionPolicySource(
    private val buildInfo: AppBuildInfo,
    private val storage: LocalStorage,
    private val logger: ILoggerService,
) : IVersionPolicySource {
    private var cached = readPersisted() ?: VersionPolicy.DISABLED

    override fun current(): VersionPolicy = cached

    override suspend fun refresh(): VersionPolicy {
        val remoteConfig = Firebase.remoteConfig
        runCatching {
            remoteConfig.settings {
                fetchTimeout = FETCH_TIMEOUT
                minimumFetchInterval = MIN_FETCH_INTERVAL
            }
        }.onFailure { error ->
            logConfig("remote_config_settings_failed ${error::class.simpleName}")
        }
        runCatching {
            remoteConfig.setDefaults(
                AppChannel.ANDROID_PUBLIC.minUsableKey to DEFAULT_THRESHOLD,
                AppChannel.ANDROID_PUBLIC.minRecommendedKey to DEFAULT_THRESHOLD,
                AppChannel.ANDROID_POS.minUsableKey to DEFAULT_THRESHOLD,
                AppChannel.ANDROID_POS.minRecommendedKey to DEFAULT_THRESHOLD,
                AppChannel.IOS_PUBLIC.minUsableKey to DEFAULT_THRESHOLD,
                AppChannel.IOS_PUBLIC.minRecommendedKey to DEFAULT_THRESHOLD,
            )
        }.onFailure { error ->
            logConfig("remote_config_defaults_failed ${error::class.simpleName}")
        }
        runCatching {
            withTimeout(FETCH_TIMEOUT.inWholeMilliseconds) {
                remoteConfig.fetchAndActivate()
            }
        }.onFailure { error ->
            logConfig("remote_config_fetch_failed ${error::class.simpleName}")
        }

        val usableRaw = runCatching {
            remoteConfig.getValue(buildInfo.channel.minUsableKey).asString()
        }.getOrNull()
        val recommendedRaw = runCatching {
            remoteConfig.getValue(buildInfo.channel.minRecommendedKey).asString()
        }.getOrNull()

        val usable = parseMarketingVersion(usableRaw)
        val recommended = parseMarketingVersion(recommendedRaw)
        if (usable == null && !usableRaw.isNullOrBlank()) {
            logConfig("invalid_usable_threshold")
        }
        if (recommended == null && !recommendedRaw.isNullOrBlank()) {
            logConfig("invalid_recommended_threshold")
        }

        val policy = normalizeVersionPolicy(
            minUsableVersion = usable ?: cached.minUsableVersion,
            minRecommendedVersion = recommended ?: cached.minRecommendedVersion,
        )
        if (policy.recommendedWasClamped) {
            logConfig("recommended_below_usable_clamped")
        }
        cached = policy
        persist(policy)
        return policy
    }

    private fun persist(policy: VersionPolicy) {
        storage.set(usableStorageKey(), policy.minUsableVersion.toString())
        storage.set(recommendedStorageKey(), policy.minRecommendedVersion.toString())
    }

    private fun readPersisted(): VersionPolicy? {
        val usable = parseMarketingVersion(storage.string(usableStorageKey())) ?: return null
        val recommended = parseMarketingVersion(storage.string(recommendedStorageKey())) ?: return null
        return normalizeVersionPolicy(usable, recommended)
    }

    private fun usableStorageKey(): String =
        "$STORAGE_PREFIX.${buildInfo.channel.name.lowercase()}.usable"

    private fun recommendedStorageKey(): String =
        "$STORAGE_PREFIX.${buildInfo.channel.name.lowercase()}.recommended"

    private fun logConfig(message: String) {
        logger.sendLog(
            Log(
                level = LogLevel.WARNING,
                flow = LOG_FLOW,
                message = message,
            )
        )
    }

    companion object {
        private const val STORAGE_PREFIX = "version_gate.policy"
        private const val LOG_FLOW = "version_gate"
        private const val DEFAULT_THRESHOLD = "0.0.0"
        private val FETCH_TIMEOUT = 8.seconds
        private val MIN_FETCH_INTERVAL = 15.minutes
    }
}
