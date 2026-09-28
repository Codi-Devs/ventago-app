package com.teco.ventago.core.version

fun normalizeVersionPolicy(
    minUsableVersion: AppVersion,
    minRecommendedVersion: AppVersion,
): VersionPolicy {
    val clamped = minRecommendedVersion < minUsableVersion
    return VersionPolicy(
        minUsableVersion = minUsableVersion,
        minRecommendedVersion = if (clamped) minUsableVersion else minRecommendedVersion,
        recommendedWasClamped = clamped,
    )
}

fun pickRemoteThreshold(primaryRaw: String?, fallbackRaw: String?): AppVersion? {
    val primary = parseMarketingVersion(primaryRaw)
    val fallback = parseMarketingVersion(fallbackRaw)
    if (primary?.isEnabled == true) return primary
    if (fallback?.isEnabled == true) return fallback
    return primary ?: fallback
}

fun readStoredVersionPolicy(
    usableRaw: String?,
    recommendedRaw: String?,
): VersionPolicy? {
    val usable = parseMarketingVersion(usableRaw) ?: return null
    val recommended = parseMarketingVersion(recommendedRaw) ?: return null
    return normalizeVersionPolicy(usable, recommended)
}

fun evaluateVersionGate(
    connected: Boolean,
    installedVersion: AppVersion?,
    policy: VersionPolicy,
): VersionGateDecision {
    if (!connected) return VersionGateDecision.Offline
    val installed = installedVersion ?: return VersionGateDecision.Allowed
    if (policy.minUsableVersion.isEnabled && installed < policy.minUsableVersion) {
        return VersionGateDecision.Forced
    }
    if (policy.minRecommendedVersion.isEnabled && installed < policy.minRecommendedVersion) {
        return VersionGateDecision.Recommended
    }
    return VersionGateDecision.Allowed
}
