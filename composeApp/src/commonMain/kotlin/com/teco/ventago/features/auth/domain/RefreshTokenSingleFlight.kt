package com.teco.ventago.features.auth.domain

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class RefreshTokenSingleFlight {
    private val mutex = Mutex()
    private var refreshFailureHandled = false

    suspend fun refresh(
        failedAccessToken: String?,
        currentAccessToken: () -> String?,
        refreshTokenAvailable: () -> Boolean,
        performRefresh: suspend () -> Unit,
        onRefreshFailure: suspend () -> Unit,
    ) {
        mutex.withLock {
            val currentToken = currentAccessToken()
            if (
                !failedAccessToken.isNullOrBlank() &&
                !currentToken.isNullOrBlank() &&
                currentToken != failedAccessToken
            ) {
                return
            }

            if (!refreshTokenAvailable()) {
                handleRefreshFailure(onRefreshFailure)
                throw IllegalStateException("Refresh token is not available")
            }

            try {
                performRefresh()
                refreshFailureHandled = false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleRefreshFailure(onRefreshFailure)
                throw e
            }
        }
    }

    fun resetFailureState() {
        refreshFailureHandled = false
    }

    private suspend fun handleRefreshFailure(onRefreshFailure: suspend () -> Unit) {
        if (refreshFailureHandled) return
        refreshFailureHandled = true
        onRefreshFailure()
    }
}
