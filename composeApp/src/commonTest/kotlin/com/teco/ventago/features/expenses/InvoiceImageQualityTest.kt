package com.teco.ventago.features.expenses

import com.teco.ventago.features.expenses.domain.InvoiceImageQuality
import com.teco.ventago.features.expenses.domain.InvoiceQualityReason
import com.teco.ventago.features.expenses.domain.InvoiceQualityVerdict
import kotlin.test.Test
import kotlin.test.assertEquals

class InvoiceImageQualityTest {
    @Test
    fun acceptsSharpDocumentSizedImage() {
        val verdict = InvoiceImageQuality.assess(640, 640, checkerboard(640, 640))
        assertEquals(InvoiceQualityVerdict.Ok, verdict)
    }

    @Test
    fun rejectsTinyImageUsingSourceSize() {
        val luma = checkerboard(120, 120)
        val verdict = InvoiceImageQuality.assess(
            width = 120,
            height = 120,
            luma = luma,
            sourceWidth = 120,
            sourceHeight = 120
        )
        assertEquals(
            InvoiceQualityReason.TOO_SMALL,
            (verdict as InvoiceQualityVerdict.Reject).reason
        )
    }

    @Test
    fun rejectsDarkFlatImage() {
        val verdict = InvoiceImageQuality.assess(640, 640, fill(640, 640, 4))
        assertEquals(
            InvoiceQualityReason.TOO_DARK,
            (verdict as InvoiceQualityVerdict.Reject).reason
        )
    }

    @Test
    fun rejectsBrightFlatImage() {
        val verdict = InvoiceImageQuality.assess(640, 640, fill(640, 640, 252))
        assertEquals(
            InvoiceQualityReason.TOO_BRIGHT,
            (verdict as InvoiceQualityVerdict.Reject).reason
        )
    }

    @Test
    fun rejectsBlurryFlatGray() {
        val verdict = InvoiceImageQuality.assess(640, 640, fill(640, 640, 120))
        assertEquals(InvoiceQualityReason.BLURRY, (verdict as InvoiceQualityVerdict.Reject).reason)
    }

    private fun fill(width: Int, height: Int, value: Int) =
        IntArray(width * height) { value }

    private fun checkerboard(width: Int, height: Int): IntArray {
        val luma = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                luma[y * width + x] = if ((x + y) % 2 == 0) 20 else 230
            }
        }
        return luma
    }
}
