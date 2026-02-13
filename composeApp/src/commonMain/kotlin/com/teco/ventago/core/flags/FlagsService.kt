package com.teco.ventago.core.flags

import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.database.DataSnapshot
import dev.gitlive.firebase.database.database
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class FlagsService(
    private val logger: ILoggerService,
    private val appScope: CoroutineScope,
) : IFlagsService {

    private val db =
        Firebase.database("https://ventago-25d2c-default-rtdb.firebaseio.com/")
    private val flagsRef = db.reference("flags")

    private val flagsState = MutableStateFlow(AppFlagsState())
    private var listenerJob: Job? = null
    private var initialized = false

    override fun flags(): StateFlow<AppFlagsState> = flagsState.asStateFlow()

    override fun initialize() {
        if (initialized) {
            log(
                level = LogLevel.WARNING,
                message = "FlagsService already initialized"
            )
            return
        }

        listenerJob?.cancel()
        listenerJob = flagsRef.valueEvents
            .onEach { snapshot ->
                if (!snapshot.exists) {
                    flagsState.value = AppFlagsState()
                    return@onEach
                }

                flagsState.value = AppFlagsState(
                    maintenanceMode = snapshot.safeBoolean("maintenance_mode"),
                    dgiDown = snapshot.safeBoolean("dgi_down")
                )
            }
            .catch { error ->
                log(
                    level = LogLevel.ERROR,
                    message = "Error listening to flags: ${error.message ?: "UNKNOWN"}"
                )
            }
            .launchIn(appScope)

        initialized = true
        log(
            level = LogLevel.INFO,
            message = "FlagsService initialized"
        )
    }

    override fun destroy() {
        listenerJob?.cancel()
        listenerJob = null
        flagsState.value = AppFlagsState()
        initialized = false
    }

    override fun isInitialized(): Boolean = initialized

    override fun getMaintenanceMode(): Boolean = flagsState.value.maintenanceMode

    override fun getDgiDown(): Boolean = flagsState.value.dgiDown

    private fun DataSnapshot.safeBoolean(key: String): Boolean {
        return runCatching {
            child(key).value<Boolean?>() ?: false
        }.getOrElse {
            log(
                level = LogLevel.WARNING,
                message = "Invalid or missing flag '$key'. Using false."
            )
            false
        }
    }

    private fun log(level: LogLevel, message: String) {
        logger.sendLog(
            Log(
                level = level,
                flow = "FlagsService",
                message = message
            )
        )
    }
}
