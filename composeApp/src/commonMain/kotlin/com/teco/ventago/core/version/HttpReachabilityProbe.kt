package com.teco.ventago.core.version

import com.teco.ventago.Configs
import com.teco.ventago.httpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.http.isSuccess

class HttpReachabilityProbe : IReachabilityProbe {
    private val probeClient = httpClient {
        expectSuccess = false
        install(HttpTimeout) {
            requestTimeoutMillis = TIMEOUT_MS
            connectTimeoutMillis = TIMEOUT_MS
            socketTimeoutMillis = TIMEOUT_MS
        }
    }

    override suspend fun isReachable(): Boolean {
        return try {
            val response = probeClient.get(Configs.ordersBasePath)
            response.status.value in 100..599 || response.status.isSuccess()
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        private const val TIMEOUT_MS = 8_000L
    }
}
