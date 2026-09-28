package com.teco.ventago.core.version

data class VersionPolicy(
    val minUsableVersion: AppVersion,
    val minRecommendedVersion: AppVersion,
    val recommendedWasClamped: Boolean = false,
) {
    companion object {
        val DISABLED = VersionPolicy(
            minUsableVersion = AppVersion.ZERO,
            minRecommendedVersion = AppVersion.ZERO,
        )
    }
}

enum class VersionGateDecision {
    Offline,
    Forced,
    Recommended,
    Allowed,
}

data class VersionGateSnapshot(
    val decision: VersionGateDecision = VersionGateDecision.Allowed,
    val policy: VersionPolicy = VersionPolicy.DISABLED,
    val installedVersion: AppVersion? = null,
    val versionName: String = "",
    val showSettingsUpdate: Boolean = false,
    val configError: String? = null,
)
