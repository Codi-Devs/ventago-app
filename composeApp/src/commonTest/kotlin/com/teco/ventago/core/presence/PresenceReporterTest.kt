package com.teco.ventago.core.presence

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PresenceReporterTest {
    @Test
    fun delayStaysInsideOneMinutePlusJitter() {
        assertEquals(45_000L, presenceDelayMillis(0.0))
        assertEquals(75_000L, presenceDelayMillis(0.999999))
    }

    @Test
    fun consumeClearsTheActivityFlag() {
        PresenceSignals.mark()
        assertTrue(PresenceSignals.consume())
        assertFalse(PresenceSignals.consume())
    }
}