package com.teco.ventago.features.expenses

import com.teco.ventago.features.expenses.domain.CrawlErrorCopy
import com.teco.ventago.features.expenses.domain.CufeParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CufeParserTest {
    private val cufe =
        "FE0120000000000000123456789012345678901234567890123456789012"

    @Test
    fun parsesDirectCufe() {
        assertEquals(cufe, CufeParser.parse("  $cufe  "))
    }

    @Test
    fun parsesFacturasPorCufePath() {
        val url = "https://dgi-fep.mef.gob.pa/Consultas/FacturasPorCUFE/$cufe"
        assertEquals(cufe, CufeParser.parse(url))
    }

    @Test
    fun parsesFacturasPorQrQuery() {
        val url = "https://dgi-fep.mef.gob.pa/Consultas/FacturasPorQR?chFE=$cufe"
        assertEquals(cufe, CufeParser.parse(url))
    }

    @Test
    fun parsesEncodedChFeQuery() {
        val url = "https://dgi-fep.mef.gob.pa/Consultas/FacturasPorQR?chFE=$cufe%20"
        assertEquals(cufe, CufeParser.parse(url))
    }

    @Test
    fun rejectsShortOrMissingCufe() {
        assertNull(CufeParser.parse("FE123"))
        assertNull(CufeParser.parse("https://example.com"))
        assertNull(CufeParser.parse(""))
    }

    @Test
    fun truncatesLongCufe() {
        assertEquals(
            "FE0120000000…56789012",
            CufeParser.truncate(cufe)
        )
    }
}

class CrawlErrorCopyTest {
    @Test
    fun keepsSpanishUserFacingCopy() {
        val message = "No se pudo consultar la factura en DGI."
        assertEquals(message, CrawlErrorCopy.userMessage(message))
    }

    @Test
    fun mapsTimeoutAndNotFound() {
        assertEquals(
            CrawlErrorCopy.TIMEOUT,
            CrawlErrorCopy.userMessage("Get \"https://dgi\": context deadline exceeded")
        )
        assertEquals(
            CrawlErrorCopy.NOT_FOUND,
            CrawlErrorCopy.userMessage("HTTP 404 not found")
        )
    }

    @Test
    fun defaultsBlankAndGoPanic() {
        assertEquals(CrawlErrorCopy.DEFAULT, CrawlErrorCopy.userMessage(null))
        assertEquals(
            CrawlErrorCopy.DEFAULT,
            CrawlErrorCopy.userMessage("panic: runtime error\ngoroutine 1 [running]:")
        )
    }
}
