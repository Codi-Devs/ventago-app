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
                AppChannel.ANDROID_PUBLIC.minUsableKey to 0L,
                AppChannel.ANDROID_PUBLIC.minRecommendedKey to 0L,
                AppChannel.ANDROID_POS.minUsableKey to 0L,
                AppChannel.ANDROID_POS.minRecommendedKey to 0L,
                AppChannel.IOS_PUBLIC.minUsableKey to 0L,
                AppChannel.IOS_PUBLIC.minRecommendedKey to 0L,
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

        val usable = parseBuildThreshold(usableRaw)
        val recommended = parseBuildThreshold(recommendedRaw)
        if (usable == null && !usableRaw.isNullOrBlank()) {
            logConfig("invalid_usable_threshold")
        }
        if (recommended == null && !recommendedRaw.isNullOrBlank()) {
            logConfig("invalid_recommended_threshold")
        }

        val policy = normalizeVersionPolicy(
            minUsableBuild = usable ?: cached.minUsableBuild,
            minRecommendedBuild = recommended ?: cached.minRecommendedBuild,
        )
        if (policy.recommendedWasClamped) {
            logConfig("recommended_below_usable_clamped")
        }
        cached = policy
        persist(policy)
        return policy
    }

    private fun persist(policy: VersionPolicy) {
        storage.set(usableStorageKey(), policy.minUsableBuild)
        storage.set(recommendedStorageKey(), policy.minRecommendedBuild)
    }

    private fun readPersisted(): VersionPolicy? {
        val usable = storage.long(usableStorageKey()) ?: return null
        val recommended = storage.long(recommendedStorageKey()) ?: return null
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
        private val FETCH_TIMEOUT = 8.seconds
        private val MIN_FETCH_INTERVAL = 15.minutes
    }
}
