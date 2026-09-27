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
