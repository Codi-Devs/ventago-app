package com.teco.ventago.features.settings

import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.features.settings.domain.reloadEmailVerification
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class EmailVerificationRefreshTest {
    @Test
    fun successfulRefreshReturnsTheServerVerificationState() = runTest {
        val logger = RecordingLogger()

        assertEquals(true, reloadEmailVerification(logger) { true })
        assertEquals(false, reloadEmailVerification(logger) { false })
        assertTrue(logger.logs.isEmpty())
    }

    @Test
    fun failedRefreshDoesNotProvideAReplacementStateOrLogSensitiveDetails() = runTest {
        val logger = RecordingLogger()

        val result = reloadEmailVerification(logger) {
            throw IllegalStateException("user@example.test sensitive-token")
        }

        assertNull(result)
        val log = logger.logs.single()
        assertEquals(LogLevel.ERROR, log.level)
        assertEquals("Settings::reloadEmailVerification", log.flow)
        assertTrue(log.message.contains("IllegalStateException"))
        assertFalse(log.message.contains("user@example.test"))
        assertFalse(log.message.contains("sensitive-token"))
    }

    @Test
    fun laterAuthEmissionCanRefreshAfterAnInternalError() = runTest {
        val logger = RecordingLogger()
        val states = mutableListOf<Boolean>()
        var verified = false

        flowOf(false, true).collect { succeeds ->
            val refreshed = reloadEmailVerification(logger) {
                if (!succeeds) throw Exception("An internal error has occurred.")
                true
            }
            if (refreshed != null) verified = refreshed
            states += verified
        }

        assertEquals(listOf(false, true), states)
        assertEquals(1, logger.logs.size)
    }

    @Test
    fun coroutineCancellationPropagatesWithoutAnErrorLog() = runTest {
        val logger = RecordingLogger()
        val cancellation = CancellationException("Screen closed")

        val thrown = assertFailsWith<CancellationException> {
            reloadEmailVerification(logger) { throw cancellation }
        }

        assertSame(cancellation, thrown)
        assertTrue(logger.logs.isEmpty())
    }

    private class RecordingLogger : ILoggerService {
        val logs = mutableListOf<Log>()

        override fun sendLog(log: Log) {
            logs += log
        }
    }
}
