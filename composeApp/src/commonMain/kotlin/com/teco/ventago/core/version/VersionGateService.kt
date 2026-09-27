package com.teco.ventago.core.version

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeout

class VersionGateService(
    private val buildInfo: AppBuildInfo,
    private val policySource: IVersionPolicySource,
    private val reachabilityProbe: IReachabilityProbe,
    private val dismissalStore: RecommendedDismissalStore,
) {
    private val _snapshot = MutableStateFlow(
        VersionGateSnapshot(
            installedBuild = buildInfo.versionCode,
            versionName = buildInfo.versionName,
        )
    )
    val snapshot: StateFlow<VersionGateSnapshot> = _snapshot.asStateFlow()

    suspend fun evaluate(): VersionGateDecision {
        val connected = runCatching {
            withTimeout(PROBE_TIMEOUT_MS) { reachabilityProbe.isReachable() }
        }.getOrDefault(false)
        val policy = if (connected) {
            runCatching { policySource.refresh() }.getOrDefault(policySource.current())
        } else {
            policySource.current()
        }
        val decision = evaluateVersionGate(
            connected = connected,
            installedBuild = buildInfo.versionCode,
            policy = policy,
        )
        _snapshot.update {
            VersionGateSnapshot(
                decision = decision,
                policy = policy,
                installedBuild = buildInfo.versionCode,
                versionName = buildInfo.versionName,
                showSettingsUpdate = buildInfo.versionCode < policy.minRecommendedBuild,
                configError = if (policy.recommendedWasClamped) "recommended_below_usable" else null,
            )
        }
        return decision
    }

    fun shouldShowRecommendedPrompt(): Boolean {
        val current = _snapshot.value
        if (current.decision != VersionGateDecision.Recommended) return false
        return !dismissalStore.isDismissed(
            recommendedBuild = current.policy.minRecommendedBuild,
            installedBuild = current.installedBuild,
        )
    }

    fun dismissRecommendedPrompt() {
        val current = _snapshot.value
        dismissalStore.dismiss(
            recommendedBuild = current.policy.minRecommendedBuild,
            installedBuild = current.installedBuild,
        )
    }

    companion object {
        private const val PROBE_TIMEOUT_MS = 8_000L
    }
}
