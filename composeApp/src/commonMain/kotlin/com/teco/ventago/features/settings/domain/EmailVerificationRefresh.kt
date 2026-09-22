package com.teco.ventago.features.settings.domain

import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import kotlinx.coroutines.CancellationException

/** A failed background refresh must not replace the last known verification state. */
internal suspend fun reloadEmailVerification(
    logger: ILoggerService,
    reload: suspend () -> Boolean,
): Boolean? = try {
    reload()
} catch (error: CancellationException) {
    throw error
} catch (error: Exception) {
    logger.sendLog(
        Log(
            LogLevel.ERROR,
            "Settings::reloadEmailVerification",
            "Firebase user refresh failed (${error::class.simpleName ?: "Exception"}); keeping the last verification state."
        )
    )
    null
}
