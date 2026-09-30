package com.teco.ventago.features.auth.domain

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertIs
import kotlin.test.assertSame

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
        assertTrue(results.all { it.exceptionOrNull() is SessionExpiredException })
    }
    @Test
    fun missingRefreshTokenExpiresSessionWithoutNetworkCallAndLogsOutOnce() = runTest {
        val singleFlight = RefreshTokenSingleFlight()
        var logoutCalls = 0
        repeat(150) {
            val result = runCatching {
                singleFlight.refresh("expired", { "expired" }, { false },
                    performRefresh = { error("Must not contact the server") },
                    onRefreshFailure = { logoutCalls++ })
            }
            assertIs<SessionExpiredException>(result.exceptionOrNull())
        }
        assertEquals(1, logoutCalls)
    }

    @Test
    fun cancelledRefreshDoesNotLogOutOrBecomeSessionExpired() = runTest {
        val singleFlight = RefreshTokenSingleFlight()
        val cancellation = CancellationException("Screen closed")
        var logoutCalls = 0
        val result = runCatching {
            singleFlight.refresh("expired", { "expired" }, { true },
                performRefresh = { throw cancellation },
                onRefreshFailure = { logoutCalls++ })
        }
        assertSame(cancellation, result.exceptionOrNull())
        assertEquals(0, logoutCalls)
    }

    @Test
    fun newLoginCanExpireAgainAfterFailureStateReset() = runTest {
        val singleFlight = RefreshTokenSingleFlight()
        var logoutCalls = 0
        repeat(2) {
            singleFlight.resetFailureState()
            val result = runCatching {
                singleFlight.refresh("expired", { "expired" }, { false },
                    performRefresh = { error("Must not refresh") },
                    onRefreshFailure = { logoutCalls++ })
            }
            assertIs<SessionExpiredException>(result.exceptionOrNull())
        }
        assertEquals(2, logoutCalls)
    }

    @Test
    fun sessionExpiryInRootLaunchDoesNotReachUncaughtExceptionHandler() = runTest {
        val supervisor = SupervisorJob()
        val uncaught = mutableListOf<Throwable>()
        val scope = CoroutineScope(coroutineContext + supervisor +
            CoroutineExceptionHandler { _, error -> uncaught.add(error) })
        var loggedOut = false
        var retriedRequest = false
        val job = scope.launch {
            RefreshTokenSingleFlight().refresh("expired", { "expired" }, { false },
                performRefresh = { error("Must not refresh") },
                onRefreshFailure = { loggedOut = true })
            retriedRequest = true
        }
        job.join()
        assertTrue(loggedOut)
        assertTrue(job.isCancelled)
        assertTrue(uncaught.isEmpty())
        assertTrue(!retriedRequest)
        supervisor.cancel()
    }

}
