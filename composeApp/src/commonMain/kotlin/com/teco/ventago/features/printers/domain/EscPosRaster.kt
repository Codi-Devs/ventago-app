package com.teco.ventago.features.printers.domain

import com.teco.ventago.features.printers.domain.model.EXTRA_NARROW_CANVAS_WIDTH_DOTS
import com.teco.ventago.features.printers.domain.model.EXTRA_NARROW_PAPER_COLUMNS
import com.teco.ventago.features.printers.domain.model.NARROW_PAPER_COLUMNS
import com.teco.ventago.features.printers.domain.model.PrintAlignment
import com.teco.ventago.features.printers.domain.model.PrintCommand
import com.teco.ventago.features.printers.domain.model.PrinterConnectionException
import com.teco.ventago.features.printers.domain.model.TicketBlockStyle
import com.teco.ventago.features.printers.domain.model.WIDE_CANVAS_WIDTH_DOTS
import com.teco.ventago.features.printers.domain.model.WIDE_PAPER_COLUMNS
import kotlin.math.abs

internal expect suspend fun renderTicketImageRaster(
    source: String,
    paperWidthDots: Int,
    widthHintPercent: Int?,
    alignment: PrintAlignment,
): ByteArray

internal expect fun renderTicketQrRaster(
    data: String,
    errorCorrection: String,
    paperWidthDots: Int,
    alignment: PrintAlignment,
): ByteArray

internal expect fun renderTicketTextRaster(
    text: String,
    style: TicketBlockStyle,
    paperWidthDots: Int,
    columns: Int,
): ByteArray

internal suspend fun commandsForBluetooth(
    commands: List<PrintCommand>,
    paperWidthDots: Int,
): List<PrintCommand> {
    val dots = paperWidthDots.coerceAtLeast(8)
    return commands.mapNotNull { command ->
        when (command) {
            is PrintCommand.Image -> {
                val source = command.source.trim()
                if (source.isEmpty()) {
                    null
                } else {
                    PrintCommand.Raster(
                        renderTicketImageRaster(
                            source = source,
                            paperWidthDots = dots,
                            widthHintPercent = command.widthHintPercent,
                            alignment = command.alignment,
                        )
                    )
                }
            }

            is PrintCommand.Text -> PrintCommand.Raster(
                renderTicketTextRaster(
                    text = command.text,
                    style = command.style,
                    paperWidthDots = dots,
                    columns = dots.toReceiptColumns(),
                )
            )

            is PrintCommand.Qr -> {
                if (command.data.isEmpty()) {
                    null
                } else {
                    PrintCommand.Raster(
                        renderTicketQrRaster(
                            data = command.data,
                            errorCorrection = command.errorCorrection,
                            paperWidthDots = dots,
                            alignment = command.alignment,
                        )
                    )
                }
            }

            else -> command
        }
    }
}

internal data class TicketGlyphLine(
    val glyphs: String,
    val cellWidth: Float,
    val cellHeight: Float,
    val textSize: Float,
    val originX: Float,
    val bitmapWidth: Int,
    val bitmapHeight: Int,
    val bold: Boolean,
    val underline: Boolean,
    val inverse: Boolean,
)

internal fun ticketGlyphLine(
    text: String,
    style: TicketBlockStyle,
    paperWidthDots: Int,
    columns: Int,
): TicketGlyphLine {
    val cols = columns.coerceAtLeast(1)
    val paper = paperWidthDots.coerceAtLeast(8)
    val baseCell = paper.toFloat() / cols
    val widthMul = if (style.doubleWidth) 2f else 1f
    val heightMul = if (style.doubleHeight) 2f else 1f
    val cellHeight = (baseCell * 2.15f * heightMul).coerceAtLeast(16f)
    val enlarged = style.doubleWidth || style.doubleHeight
    val content = if (enlarged) text.trim() else text.take(cols).padEnd(cols)
    var cellWidth = baseCell * widthMul
    if (enlarged && content.isNotEmpty() && content.length * cellWidth > paper) {
        cellWidth = paper.toFloat() / content.length
    }
    val lineWidth = content.length * cellWidth
    val originX = if (!enlarged || content.isEmpty()) {
        0f
    } else {
        val lead = text.indexOfFirst { !it.isWhitespace() }.coerceAtLeast(0)
        val trail = (text.length - lead - content.length).coerceAtLeast(0)
        when {
            lead > 1 && trail > 1 && abs(lead - trail) <= 2 -> (paper - lineWidth) / 2f
            lead > trail + 1 -> paper - lineWidth
            else -> 0f
        }
    }
    return TicketGlyphLine(
        glyphs = content,
        cellWidth = cellWidth,
        cellHeight = cellHeight,
        textSize = cellHeight * 0.78f,
        originX = originX.coerceAtLeast(0f),
        bitmapWidth = paper,
        bitmapHeight = cellHeight.toInt().coerceAtLeast(1),
        bold = style.bold,
        underline = style.underline,
        inverse = style.inverse,
    )
}

internal fun Int.toReceiptColumns(): Int = when {
    this >= WIDE_CANVAS_WIDTH_DOTS -> WIDE_PAPER_COLUMNS
    this >= EXTRA_NARROW_CANVAS_WIDTH_DOTS -> EXTRA_NARROW_PAPER_COLUMNS
    else -> NARROW_PAPER_COLUMNS
}

internal fun pixelsToEscPosRaster(
    width: Int,
    height: Int,
    isBlack: (x: Int, y: Int) -> Boolean,
    paperWidthDots: Int,
    alignment: PrintAlignment,
    feedLine: Boolean = true,
): ByteArray {
    val imageWidth = width.coerceAtLeast(1)
    val imageHeight = height.coerceAtLeast(1)
    val canvas = ((paperWidthDots.coerceAtLeast(imageWidth) + 7) / 8) * 8
    val fittedWidth = imageWidth.coerceAtMost(canvas)
    val left = when (alignment) {
        PrintAlignment.CENTER -> ((canvas - fittedWidth) / 2).coerceAtLeast(0)
        PrintAlignment.RIGHT -> (canvas - fittedWidth).coerceAtLeast(0)
        PrintAlignment.LEFT -> 0
    }
    val widthBytes = canvas / 8
    val out = ArrayList<Byte>(8 + widthBytes * imageHeight)
    out.add(0x1B)
    out.add(0x61)
    out.add(0x00)
    out.add(0x1D)
    out.add(0x76)
    out.add(0x30)
    out.add(0x00)
    out.add((widthBytes and 0xFF).toByte())
    out.add(((widthBytes shr 8) and 0xFF).toByte())
    out.add((imageHeight and 0xFF).toByte())
    out.add(((imageHeight shr 8) and 0xFF).toByte())
    for (y in 0 until imageHeight) {
        for (byteIndex in 0 until widthBytes) {
            var value = 0
            for (bit in 0 until 8) {
                val x = byteIndex * 8 + bit
                val imageX = x - left
                if (imageX in 0 until fittedWidth && isBlack(imageX, y)) {
                    value = value or (0x80 shr bit)
                }
            }
            out.add(value.toByte())
        }
    }
    if (feedLine) out.add(0x0A)
    return ByteArray(out.size) { index -> out[index] }
}

internal fun scaledModuleRaster(
    moduleWidth: Int,
    moduleHeight: Int,
    isDark: (x: Int, y: Int) -> Boolean,
    paperWidthDots: Int,
    alignment: PrintAlignment,
): ByteArray {
    val modulesWide = moduleWidth.coerceAtLeast(1)
    val modulesHigh = moduleHeight.coerceAtLeast(1)
    val scale = (paperWidthDots / modulesWide).coerceAtLeast(1)
    val width = modulesWide * scale
    val height = modulesHigh * scale
    return pixelsToEscPosRaster(
        width = width,
        height = height,
        isBlack = { x, y -> isDark(x / scale, y / scale) },
        paperWidthDots = paperWidthDots,
        alignment = alignment,
    )
}

internal fun rasterFailure(message: String, cause: Throwable? = null): Nothing {
    throw PrinterConnectionException(message, cause)
}
