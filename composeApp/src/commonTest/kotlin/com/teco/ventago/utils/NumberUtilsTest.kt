package com.teco.ventago.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NumberUtilsTest {

    @Test
    fun sanitizeQuantityInput_acceptsDecimalSeparatorsAndMaxFourDecimals() {
        assertEquals("0.", sanitizeQuantityInput("."))
        assertEquals("1.2345", sanitizeQuantityInput("1,234567"))
        assertEquals("123.45", sanitizeQuantityInput("abc123.45xyz"))
        assertEquals("", sanitizeQuantityInput("abc"))
    }

    @Test
    fun normalizeQuantity_enforcesMinAndRoundsToFourDecimals() {
        assertEquals(0.0001, normalizeQuantity(0.0))
        assertEquals(1.2346, normalizeQuantity(1.23456))
        assertEquals(2.0, normalizeQuantity(2.0))
    }

    @Test
    fun toNormalizedQuantityOrNull_parsesSanitizedInput() {
        assertEquals(0.0001, "0".toNormalizedQuantityOrNull())
        assertEquals(1.25, "1,25".toNormalizedQuantityOrNull())
        assertNull("".toNormalizedQuantityOrNull())
    }

    @Test
    fun quantityFormatting_outputsExpectedUiAndRequestShapes() {
        assertEquals("1.25", 1.2500.toQuantityUiString())
        assertEquals("2", 2.0.toQuantityUiString())
        assertEquals("1.2500", 1.25.toQuantityRequestString())
        assertEquals("2.0000", 2.0.toQuantityRequestString())
    }

    @Test
    fun multiplyCentsByQuantity_roundsHalfUpToNearestCent() {
        assertEquals(1124L, multiplyCentsByQuantity(999L, 1.125))
        assertEquals(113L, multiplyCentsByQuantity(100L, 1.125))
        assertEquals(0L, multiplyCentsByQuantity(1L, 0.0001))
    }
}
