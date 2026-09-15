package com.teco.ventago.features.expenses.domain

data class InvoiceRaster(
    val width: Int,
    val height: Int,
    val luma: IntArray,
    val sourceWidth: Int = width,
    val sourceHeight: Int = height
)

sealed class InvoiceQualityVerdict {
    data object Ok : InvoiceQualityVerdict()
    data class Reject(val reason: InvoiceQualityReason) : InvoiceQualityVerdict() {
        val message: String get() = reason.message
    }
}

enum class InvoiceQualityReason(val message: String) {
    TOO_SMALL("La imagen es demasiado pequeña. Toma una foto más cercana de la factura."),
    BLURRY("La foto está borrosa. Toma otra con buena luz, sin mover el teléfono."),
    TOO_DARK("La foto está demasiado oscura. Mejora la luz y vuelve a intentar."),
    TOO_BRIGHT("La foto está quemada por la luz. Evita el flash directo y toma otra."),
    UNREADABLE("No se pudo leer la imagen. Prueba con otra foto o un PDF.")
}

object InvoiceImageQuality {
    const val MIN_SHORT_SIDE = 400
    const val ANALYZE_MAX_SIDE = 480
    const val MIN_LAPLACIAN_VARIANCE = 22.0
    const val MIN_MEAN_LUMA = 16.0
    const val MAX_MEAN_LUMA = 245.0

    fun assess(raster: InvoiceRaster): InvoiceQualityVerdict {
        return assess(
            width = raster.width,
            height = raster.height,
            luma = raster.luma,
            sourceWidth = raster.sourceWidth,
            sourceHeight = raster.sourceHeight
        )
    }

    fun assess(
        width: Int,
        height: Int,
        luma: IntArray,
        sourceWidth: Int = width,
        sourceHeight: Int = height
    ): InvoiceQualityVerdict {
        if (width < 2 || height < 2 || luma.size < width * height) {
            return InvoiceQualityVerdict.Reject(InvoiceQualityReason.UNREADABLE)
        }
        if (minOf(sourceWidth, sourceHeight) < MIN_SHORT_SIDE) {
            return InvoiceQualityVerdict.Reject(InvoiceQualityReason.TOO_SMALL)
        }
        val stats = laplacianStats(luma, width, height)
        return when {
            stats.meanLuma < MIN_MEAN_LUMA ->
                InvoiceQualityVerdict.Reject(InvoiceQualityReason.TOO_DARK)
            stats.meanLuma > MAX_MEAN_LUMA ->
                InvoiceQualityVerdict.Reject(InvoiceQualityReason.TOO_BRIGHT)
            stats.variance < MIN_LAPLACIAN_VARIANCE ->
                InvoiceQualityVerdict.Reject(InvoiceQualityReason.BLURRY)
            else -> InvoiceQualityVerdict.Ok
        }
    }

    fun downsampleArgb(argb: IntArray, width: Int, height: Int, maxSide: Int = ANALYZE_MAX_SIDE): InvoiceRaster {
        val scale = minOf(1f, maxSide.toFloat() / maxOf(width, height).toFloat())
        val outW = maxOf(1, kotlin.math.round(width * scale).toInt())
        val outH = maxOf(1, kotlin.math.round(height * scale).toInt())
        val luma = IntArray(outW * outH)
        for (y in 0 until outH) {
            val srcY = minOf(height - 1, (y / scale).toInt())
            for (x in 0 until outW) {
                val srcX = minOf(width - 1, (x / scale).toInt())
                val pixel = argb[srcY * width + srcX]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                luma[y * outW + x] = ((0.299 * r) + (0.587 * g) + (0.114 * b)).toInt()
            }
        }
        return InvoiceRaster(outW, outH, luma, sourceWidth = width, sourceHeight = height)
    }

    private fun laplacianStats(luma: IntArray, width: Int, height: Int): LaplacianStats {
        var lumaSum = 0.0
        val pixelCount = width * height
        for (i in 0 until pixelCount) {
            lumaSum += luma[i]
        }
        val meanLuma = lumaSum / pixelCount
        if (width < 3 || height < 3) {
            return LaplacianStats(meanLuma, 0.0)
        }
        var sum = 0.0
        var sumSq = 0.0
        var n = 0
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val c = luma[y * width + x]
                val lap = (4 * c) -
                    luma[y * width + x - 1] -
                    luma[y * width + x + 1] -
                    luma[(y - 1) * width + x] -
                    luma[(y + 1) * width + x]
                sum += lap
                sumSq += lap * lap.toDouble()
                n++
            }
        }
        val meanLap = sum / n
        return LaplacianStats(meanLuma, (sumSq / n) - meanLap * meanLap)
    }

    private data class LaplacianStats(val meanLuma: Double, val variance: Double)
}
