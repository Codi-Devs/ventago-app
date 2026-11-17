package com.teco.ventago.core.presence

import com.teco.ventago.AppInfo
import com.teco.ventago.Configs
import com.teco.ventago.clientDeviceClass
import com.teco.ventago.core.session.ClientRequestIdentity
import com.teco.ventago.core.session.ISessionIdService
import com.teco.ventago.core.session.SessionIdConstants
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.httpClient
import com.teco.ventago.isAndroid
import com.teco.ventago.isIOS
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.max
import kotlin.random.Random

object PresenceSignals {
    private val dirty = MutableStateFlow(false)

    fun mark() {
        dirty.value = true
    }

    fun consume(): Boolean = dirty.compareAndSet(true, false)
}

fun presenceDelayMillis(randomUnit: Double): Long {
    val jitter = (randomUnit.coerceIn(0.0, 0.999999) * 30001.0).toInt() - 15000
    return max(15_000L, 60_000L + jitter)
}

class PresenceReporter(
    private val authService: IAuthService,
    private val sessionIdService: ISessionIdService,
    private val randomUnit: () -> Double = { Random.nextDouble() },
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loop: Job? = null
    private val client = httpClient {
        install(io.ktor.client.plugins.HttpTimeout) {
            requestTimeoutMillis = 5_000
            connectTimeoutMillis = 3_000
            socketTimeoutMillis = 5_000
        }
    }

    fun onForeground() {
        if (loop?.isActive == true) {
            return
        }
        loop = scope.launch { runLoop() }
    }

    fun onBackground() {
        loop?.cancel()
        loop = null
    }

    private suspend fun runLoop() {
        while (currentCoroutineContext().isActive) {
            delay(presenceDelayMillis(randomUnit()))
            if (!PresenceSignals.consume()) {
                continue
            }
            sendBeat()
        }
    }

    private suspend fun sendBeat() {
        val token = authService.getJwtToken().orEmpty()
        if (token.isBlank()) {
            return
        }
        val session = runCatching { sessionIdService.sessionIdForBackendRequest() }.getOrNull() ?: return
        runCatching {
            client.post("${Configs.ordersBasePath}/api/v1/presence/beat") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.Accept, "*/*")
                header(
                    HttpHeaders.UserAgent,
                    ClientRequestIdentity.userAgent(AppInfo.VERSION, isAndroid(), isIOS()),
                )
                header(ClientRequestIdentity.CHANNEL_HEADER, "app")
                header(ClientRequestIdentity.OS_HEADER, ClientRequestIdentity.osHeader(isAndroid(), isIOS()))
                header(ClientRequestIdentity.DEVICE_HEADER, clientDeviceClass())
                header(SessionIdConstants.HEADER_NAME, session)
            }
        }
    }
}
