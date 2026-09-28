package com.teco.ventago.core.version

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VersionGateEvaluatorTest {

    @Test
    fun offlineWinsEvenWithCachedForcedPolicy() {
        val decision = evaluateVersionGate(
            connected = false,
            installedVersion = v("1.0.0"),
            policy = policy("1.6.0", "1.6.7"),
        )
        assertEquals(VersionGateDecision.Offline, decision)
    }

    @Test
    fun forcedWhenBelowUsable() {
        val decision = evaluateVersionGate(
            connected = true,
            installedVersion = v("1.6.6"),
            policy = policy("1.6.7", "1.6.8"),
        )
        assertEquals(VersionGateDecision.Forced, decision)
    }

    @Test
    fun recommendedWhenBetweenThresholds() {
        val decision = evaluateVersionGate(
            connected = true,
            installedVersion = v("1.6.7"),
            policy = policy("1.6.7", "1.6.8"),
        )
        assertEquals(VersionGateDecision.Recommended, decision)
    }

    @Test
    fun allowedWhenAtRecommended() {
        val decision = evaluateVersionGate(
            connected = true,
            installedVersion = v("1.6.8"),
            policy = policy("1.6.7", "1.6.8"),
        )
        assertEquals(VersionGateDecision.Allowed, decision)
    }

    @Test
    fun defaultsZeroNeverForceOrRecommend() {
        val decision = evaluateVersionGate(
            connected = true,
            installedVersion = v("1.0.0"),
            policy = VersionPolicy.DISABLED,
        )
        assertEquals(VersionGateDecision.Allowed, decision)
    }

    @Test
    fun unparseableInstalledVersionFailsOpenWhenOnline() {
        val decision = evaluateVersionGate(
            connected = true,
            installedVersion = null,
            policy = policy("1.6.7", "1.6.8"),
        )
        assertEquals(VersionGateDecision.Allowed, decision)
    }

    @Test
    fun posSuffixComparesAsMarketingVersion() {
        assertEquals(v("1.6.7"), parseMarketingVersion("1.6.7-pos"))
        assertEquals(
            VersionGateDecision.Allowed,
            evaluateVersionGate(
                connected = true,
                installedVersion = parseMarketingVersion("1.6.7-pos"),
                policy = policy("1.6.7", "1.6.7"),
            ),
        )
    }

    @Test
    fun parseRejectsBlankBuildNumbersAndGarbage() {
        assertNull(parseMarketingVersion(null))
        assertNull(parseMarketingVersion(""))
        assertNull(parseMarketingVersion("  "))
        assertNull(parseMarketingVersion("-1"))
        assertNull(parseMarketingVersion("53"))
        assertNull(parseMarketingVersion("abc"))
        assertNull(parseMarketingVersion("1.6.7.1"))
        assertEquals(AppVersion.ZERO, parseMarketingVersion("0"))
        assertEquals(AppVersion.ZERO, parseMarketingVersion("0.0.0"))
        assertEquals(v("1.6.0"), parseMarketingVersion("1.6"))
        assertEquals(v("1.6.7"), parseMarketingVersion("1.6.7"))
    }

    @Test
    fun recommendedBelowUsableIsClamped() {
        val policy = normalizeVersionPolicy(
            minUsableVersion = v("1.6.8")!!,
            minRecommendedVersion = v("1.6.5")!!,
        )
        assertTrue(policy.recommendedWasClamped)
        assertEquals(v("1.6.8"), policy.minUsableVersion)
        assertEquals(v("1.6.8"), policy.minRecommendedVersion)
        assertEquals(
            VersionGateDecision.Allowed,
            evaluateVersionGate(connected = true, installedVersion = v("1.6.8"), policy = policy),
        )
        assertEquals(
            VersionGateDecision.Forced,
            evaluateVersionGate(connected = true, installedVersion = v("1.6.7"), policy = policy),
        )
    }

    @Test
    fun pickRemoteThresholdPrefersLegacyBuildKeyWhenVersionKeyIsDefault() {
        assertEquals(v("1.6.6"), pickRemoteThreshold("0.0.0", "1.6.6"))
        assertEquals(v("1.6.8"), pickRemoteThreshold("1.6.8", "1.6.6"))
        assertNull(pickRemoteThreshold(null, null))
    }

    @Test
    fun storedPolicyIgnoresMissingAndLegacyBuildNumbers() {
        assertNull(readStoredVersionPolicy(null, null))
        assertNull(readStoredVersionPolicy("53", "54"))
        val policy = readStoredVersionPolicy("1.6.7", "1.6.8")
        assertEquals(v("1.6.7"), policy?.minUsableVersion)
        assertEquals(v("1.6.8"), policy?.minRecommendedVersion)
    }

    @Test
    fun channelsKeepIndependentKeys() {
        assertFalse(AppChannel.ANDROID_PUBLIC.minUsableKey == AppChannel.ANDROID_POS.minUsableKey)
        assertFalse(AppChannel.ANDROID_PUBLIC.minRecommendedKey == AppChannel.IOS_PUBLIC.minRecommendedKey)
        assertTrue(AppChannel.ANDROID_PUBLIC.minUsableKey.contains("version"))
        assertEquals(
            "app_min_recommended_build_android_public",
            AppChannel.ANDROID_PUBLIC.minRecommendedLegacyKey,
        )
        assertEquals(
            AppChannel.ANDROID_POS,
            AppChannel.resolve(
                packageName = AppChannel.ANDROID_POS_PACKAGE,
                isPosBuild = true,
                isIos = false,
            ),
        )
        assertEquals(
            AppChannel.ANDROID_PUBLIC,
            AppChannel.resolve(
                packageName = AppChannel.ANDROID_PUBLIC_PACKAGE,
                isPosBuild = false,
                isIos = false,
            ),
        )
        assertEquals(
            AppChannel.IOS_PUBLIC,
            AppChannel.resolve(
                packageName = AppChannel.IOS_PUBLIC_BUNDLE,
                isPosBuild = false,
                isIos = true,
            ),
        )
    }

    private fun v(raw: String): AppVersion? = parseMarketingVersion(raw)

    private fun policy(usable: String, recommended: String): VersionPolicy {
        return normalizeVersionPolicy(
            minUsableVersion = v(usable)!!,
            minRecommendedVersion = v(recommended)!!,
        )
    }
}
