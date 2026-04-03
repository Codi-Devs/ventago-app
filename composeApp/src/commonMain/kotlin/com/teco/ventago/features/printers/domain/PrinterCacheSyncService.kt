package com.teco.ventago.features.printers.domain

import com.teco.ventago.core.LocalStorage
import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.database.database
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PrinterCacheSyncService(
    private val storage: LocalStorage,
    private val logger: ILoggerService,
    private val appScope: CoroutineScope,
) {
    private val db = Firebase.database("https://ventago-25d2c-default-rtdb.firebaseio.com/")
    private var listenerJob: Job? = null
    private var currentBusinessId: Int? = null
    private val refreshMutex = Mutex()
    private var inFlightRefreshBusinessId: Int? = null

    fun start(
        businessId: Int,
        onForceRefresh: suspend (Int) -> Boolean,
    ) {
        if (listenerJob != null && currentBusinessId == businessId) return
        stop()
        currentBusinessId = businessId
        val tokenKey = tokenKey(businessId)

        listenerJob = db.reference("changes/$businessId").valueEvents.onEach { snapshot ->
            val remoteToken = snapshot.child("changed_printer").value<String?>()?.takeIf { it.isNotBlank() } ?: return@onEach
            val localToken = storage.string(tokenKey)
            if (localToken == null) {
                storage.set(tokenKey, remoteToken)
                return@onEach
            }
            if (remoteToken == localToken) {
                return@onEach
            }

            appScope.launch {
                refreshMutex.withLock {
                    if (inFlightRefreshBusinessId == businessId) return@withLock
                    inFlightRefreshBusinessId = businessId
                }
                val refreshed = runCatching { onForceRefresh(businessId) }
                    .onFailure {
                        logger.sendLog(
                            Log(
                                LogLevel.ERROR,
                                "PrinterCacheSyncService",
                                "Forced printer refresh failed. businessId=$businessId error=${it.message ?: "UNKNOWN"}"
                            )
                        )
                    }
                    .getOrDefault(false)

                if (refreshed) {
                    storage.set(tokenKey, remoteToken)
                }
                refreshMutex.withLock {
                    if (inFlightRefreshBusinessId == businessId) {
                        inFlightRefreshBusinessId = null
                    }
                }
            }
        }.launchIn(appScope)
    }

    fun stop() {
        listenerJob?.cancel()
        listenerJob = null
        currentBusinessId = null
        inFlightRefreshBusinessId = null
    }

    companion object {
        fun tokenKey(businessId: Int): String = "cache:changed_printer:$businessId"
    }
}

