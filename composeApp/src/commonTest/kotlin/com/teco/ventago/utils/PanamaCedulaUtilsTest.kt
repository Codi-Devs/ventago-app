package com.teco.ventago.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PanamaCedulaUtilsTest {

    @Test
    fun isValidPanamaCedula_acceptsAllSupportedFormats() {
        val validCedulas = listOf(
            "1-1234-12345",
            "12-1234-12345",
            "8-888-8456",
            "8-888-846",
            "8-88-8456",
            "8-8888-8456",
            "8-888-84",
            "PE-123-12345",
            "E-1234-12345",
            "N-12345-1234",
            "1AV1234-12345",
            "1PI-1234-1234",
            "pe-1234-12345",
            "PE-1234-12345",
            "E-1234-123456",
            "N-1234-1234",
            "1AV-1234-12345",
            "1PI-1234-12345",
        )

        validCedulas.forEach { cedula ->
            assertTrue(isValidPanamaCedula(cedula), "Expected valid cédula: $cedula")
        }
    }

    @Test
    fun isValidPanamaCedula_rejectsInvalidFormats() {
        val invalidCedulas = listOf(
            "00-00-0000",
            "0-1234-12345",
            "PE-12-12345",
            "E-123-12345",
            "N-123-1234",
            "1AV-123-12345",
            "1PI-123-1234",
            "A8-888-846",
        )

        invalidCedulas.forEach { cedula ->
            assertFalse(isValidPanamaCedula(cedula), "Expected invalid cédula: $cedula")
        }
    }

    @Test
    fun normalizePanamaCedula_trimsAndUppercasesInput() {
        assertEquals("PE-1234-12345", normalizePanamaCedula("  pe-1234-12345  "))
        assertTrue(isValidPanamaCedula(normalizePanamaCedula("  pe-1234-12345  ")))
    }
}
