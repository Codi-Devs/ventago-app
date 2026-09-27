package com.teco.ventago.core.version

data class VersionPolicy(
    val minUsableBuild: Long,
    val minRecommendedBuild: Long,
    val recommendedWasClamped: Boolean = false,
) {
    companion object {
        val DISABLED = VersionPolicy(minUsableBuild = 0L, minRecommendedBuild = 0L)
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
    val installedBuild: Long = 0L,
    val versionName: String = "",
    val showSettingsUpdate: Boolean = false,
    val configError: String? = null,
)
