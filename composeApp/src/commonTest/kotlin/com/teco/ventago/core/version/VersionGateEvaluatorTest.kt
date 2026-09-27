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
            installedBuild = 10L,
            policy = VersionPolicy(minUsableBuild = 50L, minRecommendedBuild = 60L),
        )
        assertEquals(VersionGateDecision.Offline, decision)
    }

    @Test
    fun forcedWhenBelowUsable() {
        val decision = evaluateVersionGate(
            connected = true,
            installedBuild = 49L,
            policy = VersionPolicy(minUsableBuild = 50L, minRecommendedBuild = 60L),
        )
        assertEquals(VersionGateDecision.Forced, decision)
    }

    @Test
    fun recommendedWhenBetweenThresholds() {
        val decision = evaluateVersionGate(
            connected = true,
            installedBuild = 50L,
            policy = VersionPolicy(minUsableBuild = 50L, minRecommendedBuild = 60L),
        )
        assertEquals(VersionGateDecision.Recommended, decision)
    }

    @Test
    fun allowedWhenAtRecommended() {
        val decision = evaluateVersionGate(
            connected = true,
            installedBuild = 60L,
            policy = VersionPolicy(minUsableBuild = 50L, minRecommendedBuild = 60L),
        )
        assertEquals(VersionGateDecision.Allowed, decision)
    }

    @Test
    fun defaultsZeroNeverForceOrRecommend() {
        val decision = evaluateVersionGate(
            connected = true,
            installedBuild = 1L,
            policy = VersionPolicy.DISABLED,
        )
        assertEquals(VersionGateDecision.Allowed, decision)
    }

    @Test
    fun parseRejectsBlankNegativeAndNonInteger() {
        assertNull(parseBuildThreshold(null))
        assertNull(parseBuildThreshold(""))
        assertNull(parseBuildThreshold("  "))
        assertNull(parseBuildThreshold("-1"))
        assertNull(parseBuildThreshold("1.6.7"))
        assertNull(parseBuildThreshold("abc"))
        assertEquals(0L, parseBuildThreshold("0"))
        assertEquals(53L, parseBuildThreshold("53"))
    }

    @Test
    fun recommendedBelowUsableIsClamped() {
        val policy = normalizeVersionPolicy(minUsableBuild = 80L, minRecommendedBuild = 50L)
        assertTrue(policy.recommendedWasClamped)
        assertEquals(80L, policy.minUsableBuild)
        assertEquals(80L, policy.minRecommendedBuild)
        assertEquals(
            VersionGateDecision.Allowed,
            evaluateVersionGate(connected = true, installedBuild = 80L, policy = policy),
        )
        assertEquals(
            VersionGateDecision.Forced,
            evaluateVersionGate(connected = true, installedBuild = 79L, policy = policy),
        )
    }

    @Test
    fun channelsKeepIndependentKeys() {
        assertFalse(AppChannel.ANDROID_PUBLIC.minUsableKey == AppChannel.ANDROID_POS.minUsableKey)
        assertFalse(AppChannel.ANDROID_PUBLIC.minRecommendedKey == AppChannel.IOS_PUBLIC.minRecommendedKey)
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
}
