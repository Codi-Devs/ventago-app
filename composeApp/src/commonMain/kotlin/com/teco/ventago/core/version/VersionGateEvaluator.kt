package com.teco.ventago.core.version

fun parseBuildThreshold(raw: String?): Long? {
    val trimmed = raw?.trim().orEmpty()
    if (trimmed.isEmpty()) return null
    val parsed = trimmed.toLongOrNull() ?: return null
    if (parsed < 0L) return null
    return parsed
}

fun normalizeVersionPolicy(
    minUsableBuild: Long,
    minRecommendedBuild: Long,
): VersionPolicy {
    val clamped = minRecommendedBuild < minUsableBuild
    return VersionPolicy(
        minUsableBuild = minUsableBuild,
        minRecommendedBuild = if (clamped) minUsableBuild else minRecommendedBuild,
        recommendedWasClamped = clamped,
    )
}

fun evaluateVersionGate(
    connected: Boolean,
    installedBuild: Long,
    policy: VersionPolicy,
): VersionGateDecision {
    if (!connected) return VersionGateDecision.Offline
    if (installedBuild < policy.minUsableBuild) return VersionGateDecision.Forced
    if (installedBuild < policy.minRecommendedBuild) return VersionGateDecision.Recommended
    return VersionGateDecision.Allowed
}
