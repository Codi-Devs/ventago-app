package com.teco.ventago.features.home

import com.teco.ventago.features.home.domain.HomeSummaryCachePolicy
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeSummaryCachePolicyTest {

    @Test
    fun cacheHitDoesNotCallApi() {
        assertFalse(HomeSummaryCachePolicy.shouldCallApi(force = false, hasCachedPayload = true))
    }

    @Test
    fun missingCacheCallsApi() {
        assertTrue(HomeSummaryCachePolicy.shouldCallApi(force = false, hasCachedPayload = false))
    }

    @Test
    fun realtimeUuidChangeForcesApi() {
        assertTrue(HomeSummaryCachePolicy.shouldCallApi(force = true, hasCachedPayload = true))
    }
}
