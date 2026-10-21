package com.teco.ventago.features.printers

import com.teco.ventago.features.printers.domain.EscPosTicketEncoder
import com.teco.ventago.features.printers.domain.model.PrintAlignment
import com.teco.ventago.features.printers.domain.model.PrintCommand
import com.teco.ventago.features.printers.domain.model.TicketBlockStyle
import com.teco.ventago.features.printers.domain.pixelsToEscPosRaster
import com.teco.ventago.features.printers.domain.ticketGlyphLine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EscPosTicketEncoderTest {
    @Test
    fun encode_initializesAndCentersAsciiText() {
        val bytes = EscPosTicketEncoder.encode(
            listOf(PrintCommand.Text("VentaGO", alignment = PrintAlignment.CENTER)),
        )
        assertTrue(bytes.hasSequence(0x1B, 0x40))
        assertTrue(bytes.hasSequence(0x1B, 0x74, 0x02))
        assertTrue(bytes.hasSequence(0x1B, 0x61, 0x01))
        assertTrue(bytes.decodeToString().contains("VentaGO"))
    }

    @Test
    fun encode_mapsSpanishEnieToPc850() {
        val bytes = EscPosTicketEncoder.encode(listOf(PrintCommand.Text("niño")))
        assertTrue(bytes.any { it == 0xA4.toByte() })
    }

    @Test
    fun encode_keepsAccentedSpanishAndSelectsPosFont() {
        val bytes = EscPosTicketEncoder.encode(listOf(PrintCommand.Text("Operación")))
        assertTrue(bytes.hasSequence(0x1C, 0x2E))
        assertTrue(bytes.hasSequence(0x1B, 0x74, 0x02))
        assertTrue(bytes.hasSequence(0x1B, 0x4D, 0x00))
        assertTrue(bytes.any { it == 0xA2.toByte() })
    }

    @Test
    fun raster_marksTheLeftmostPixelBlack() {
        val bytes = pixelsToEscPosRaster(
            width = 8,
            height = 1,
            isBlack = { x, _ -> x == 0 },
            paperWidthDots = 8,
            alignment = PrintAlignment.LEFT,
        )
        assertTrue(bytes.hasSequence(0x1D, 0x76, 0x30, 0x00, 0x01, 0x00, 0x01, 0x00))
        assertEquals(0x80.toByte(), bytes[bytes.lastIndex - 1])
    }

    @Test
    fun encode_cutFeedsPaperInsteadOfUsingACutter() {
        val bytes = EscPosTicketEncoder.encode(listOf(PrintCommand.Cut()))
        assertTrue(bytes.hasSequence(0x1B, 0x64, 0x04))
        assertFalse(bytes.hasSequence(0x1D, 0x56))
    }

    @Test
    fun encode_writesEscPosQr() {
        val bytes = EscPosTicketEncoder.encode(listOf(PrintCommand.Qr(data = "AB")))
        assertTrue(bytes.hasSequence(0x1D, 0x28, 0x6B))
        assertTrue(bytes.decodeToString().contains("AB"))
    }

    @Test
    fun glyphLine_keepsAccentsAndGrowsWhenDoubled() {
        val normal = ticketGlyphLine("Operación", TicketBlockStyle(), 384, 32)
        val bold = ticketGlyphLine("Operación", TicketBlockStyle(bold = true), 384, 32)
        val large = ticketGlyphLine(
            "Operación",
            TicketBlockStyle(doubleWidth = true, doubleHeight = true),
            384,
            32,
        )
        assertTrue(normal.glyphs.contains("ó"))
        assertTrue(bold.bold)
        assertFalse(normal.bold)
        assertTrue(large.bitmapHeight > normal.bitmapHeight)
        assertTrue(large.textSize > normal.textSize)
    }
}

private fun ByteArray.hasSequence(vararg values: Int): Boolean {
    if (values.isEmpty() || size < values.size) return false
    val needle = values.map { it.toByte() }
    return toList().windowed(needle.size).any { it == needle }
}
