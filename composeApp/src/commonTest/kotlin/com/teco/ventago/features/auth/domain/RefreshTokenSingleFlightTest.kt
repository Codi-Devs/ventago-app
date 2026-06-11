package com.teco.ventago.features.auth.domain

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RefreshTokenSingleFlightTest {

    @Test
    fun concurrentRefreshesRunOneNetworkRefreshAndShareResult() = runTest {
        val singleFlight = RefreshTokenSingleFlight()
        val releaseRefresh = CompletableDeferred<Unit>()
        var accessToken: String? = "expired-access"
        var refreshCalls = 0
        var logoutCalls = 0

        val requests = List(150) {
            async {
                singleFlight.refresh(
                    failedAccessToken = "expired-access",
                    currentAccessToken = { accessToken },
                    refreshTokenAvailable = { true },
                    performRefresh = {
                        refreshCalls += 1
                        releaseRefresh.await()
                        accessToken = "fresh-access"
                    },
                    onRefreshFailure = { logoutCalls += 1 },
                )
            }
        }

        yield()
        releaseRefresh.complete(Unit)
        requests.awaitAll()

        assertEquals(1, refreshCalls)
        assertEquals(0, logoutCalls)
        assertEquals("fresh-access", accessToken)
    }

    @Test
    fun concurrentRefreshFailuresRunOneNetworkRefreshAndOneLogout() = runTest {
        val singleFlight = RefreshTokenSingleFlight()
        var accessToken: String? = "expired-access"
        var hasRefreshToken = true
        var refreshCalls = 0
        var logoutCalls = 0

        val results = List(150) {
            async {
                runCatching {
                    singleFlight.refresh(
                        failedAccessToken = "expired-access",
                        currentAccessToken = { accessToken },
                        refreshTokenAvailable = { hasRefreshToken },
                        performRefresh = {
                            refreshCalls += 1
                            throw IllegalStateException("refresh failed")
                        },
                        onRefreshFailure = {
                            logoutCalls += 1
                            accessToken = null
                            hasRefreshToken = false
                        },
                    )
                }
            }
        }.awaitAll()

        assertEquals(1, refreshCalls)
        assertEquals(1, logoutCalls)
        assertTrue(results.all { it.isFailure })
    }
}
