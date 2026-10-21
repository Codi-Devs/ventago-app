package com.teco.ventago.features.printers.domain

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder
import com.teco.ventago.features.printers.domain.model.PrintAlignment
import com.teco.ventago.features.printers.domain.model.TicketBlockStyle
import java.net.HttpURLConnection
import java.net.URL
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal actual suspend fun renderTicketImageRaster(
    source: String,
    paperWidthDots: Int,
    widthHintPercent: Int?,
    alignment: PrintAlignment,
): ByteArray = withContext(Dispatchers.IO) {
    val bytes = loadTicketImageBytes(source)
    val sourceBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        ?: rasterFailure("No se pudo decodificar el logo del negocio")
    val scaled = sourceBitmap.scaleToPaper(paperWidthDots, widthHintPercent)
    try {
        val pixels = IntArray(scaled.width * scaled.height)
        scaled.getPixels(pixels, 0, scaled.width, 0, 0, scaled.width, scaled.height)
        pixelsToEscPosRaster(
            width = scaled.width,
            height = scaled.height,
            isBlack = { x, y -> pixels[y * scaled.width + x].isThermalBlack() },
            paperWidthDots = paperWidthDots,
            alignment = alignment,
        )
    } finally {
        if (scaled !== sourceBitmap) scaled.recycle()
        sourceBitmap.recycle()
    }
}

internal actual fun renderTicketQrRaster(
    data: String,
    errorCorrection: String,
    paperWidthDots: Int,
    alignment: PrintAlignment,
): ByteArray {
    val level = when (errorCorrection.trim().uppercase()) {
        "L" -> ErrorCorrectionLevel.L
        "Q" -> ErrorCorrectionLevel.Q
        "H" -> ErrorCorrectionLevel.H
        else -> ErrorCorrectionLevel.M
    }
    val qr = try {
        Encoder.encode(data, level, mapOf(EncodeHintType.CHARACTER_SET to "UTF-8"))
    } catch (error: Exception) {
        rasterFailure("No se pudo generar el QR del ticket", error)
    }
    val matrix = qr.matrix ?: rasterFailure("No se pudo generar el QR del ticket")
    val quiet = 1
    val side = matrix.width + quiet * 2
    return scaledModuleRaster(
        moduleWidth = side,
        moduleHeight = matrix.height + quiet * 2,
        isDark = { x, y ->
            val moduleX = x - quiet
            val moduleY = y - quiet
            moduleX in 0 until matrix.width &&
                moduleY in 0 until matrix.height &&
                matrix.get(moduleX, moduleY).toInt() == 1
        },
        paperWidthDots = paperWidthDots,
        alignment = alignment,
    )
}

internal actual fun renderTicketTextRaster(
    text: String,
    style: TicketBlockStyle,
    paperWidthDots: Int,
    columns: Int,
): ByteArray {
    val line = ticketGlyphLine(text, style, paperWidthDots, columns)
    val bitmap = Bitmap.createBitmap(line.bitmapWidth, line.bitmapHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(if (line.inverse) Color.BLACK else Color.WHITE)
    if (line.glyphs.isNotEmpty()) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (line.inverse) Color.WHITE else Color.BLACK
            typeface = Typeface.create(
                Typeface.MONOSPACE,
                if (line.bold) Typeface.BOLD else Typeface.NORMAL,
            )
            textSize = line.textSize
            isUnderlineText = line.underline
        }
        if (line.bold) {
            paint.style = Paint.Style.FILL_AND_STROKE
            paint.strokeWidth = (line.cellHeight * 0.014f).coerceIn(0.3f, 0.45f)
        }
        while (paint.measureText("M") > line.cellWidth * 0.94f && paint.textSize > 6f) {
            paint.textSize *= 0.96f
        }
        val baseline = line.bitmapHeight * 0.78f
        line.glyphs.forEachIndexed { index, character ->
            if (character == ' ') return@forEachIndexed
            val glyph = character.toString()
            val measured = paint.measureText(glyph)
            val x = line.originX + index * line.cellWidth + (line.cellWidth - measured) / 2f
            canvas.drawText(glyph, x, baseline, paint)
        }
    }
    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    bitmap.recycle()
    return pixelsToEscPosRaster(
        width = line.bitmapWidth,
        height = line.bitmapHeight,
        isBlack = { x, y ->
            pixels[y * line.bitmapWidth + x].isInk(if (line.inverse) 80 else 210)
        },
        paperWidthDots = line.bitmapWidth,
        alignment = PrintAlignment.LEFT,
        feedLine = false,
    )
}

@OptIn(ExperimentalEncodingApi::class)
private fun loadTicketImageBytes(source: String): ByteArray {
    val trimmed = source.trim()
    return if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
        val connection = (URL(trimmed).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            instanceFollowRedirects = true
        }
        try {
            connection.inputStream.use { input -> input.readBytes() }
        } catch (error: Exception) {
            rasterFailure("No se pudo descargar el logo del negocio", error)
        } finally {
            connection.disconnect()
        }
    } else {
        try {
            Base64.decode(trimmed.substringAfter("base64,", trimmed))
        } catch (error: Exception) {
            rasterFailure("No se pudo leer el logo del negocio", error)
        }
    }
}

private fun Bitmap.scaleToPaper(paperWidthDots: Int, widthHintPercent: Int?): Bitmap {
    val maxWidth = paperWidthDots.coerceAtLeast(1)
    val requested = widthHintPercent
        ?.coerceIn(1, 100)
        ?.let { (maxWidth * it / 100.0).roundToInt() }
        ?: width
    val targetWidth = requested.coerceIn(1, maxWidth)
    val targetHeight = (height * (targetWidth.toFloat() / width.coerceAtLeast(1))).roundToInt().coerceIn(1, 800)
    if (targetWidth == width && targetHeight == height) return this
    val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    canvas.drawColor(Color.WHITE)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    canvas.drawBitmap(this, null, Rect(0, 0, targetWidth, targetHeight), paint)
    return output
}

private fun Int.isThermalBlack(): Boolean = isInk(180)

private fun Int.isInk(threshold: Int): Boolean {
    val alpha = (this ushr 24) and 0xFF
    if (alpha < 40) return false
    val red = (this ushr 16) and 0xFF
    val green = (this ushr 8) and 0xFF
    val blue = this and 0xFF
    return (red * 30 + green * 59 + blue * 11) / 100 < threshold
}
