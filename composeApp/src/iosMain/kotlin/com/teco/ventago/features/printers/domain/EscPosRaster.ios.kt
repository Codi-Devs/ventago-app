@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.teco.ventago.features.printers.domain

import com.teco.ventago.features.printers.domain.model.PrintAlignment
import com.teco.ventago.features.printers.domain.model.TicketBlockStyle
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.roundToInt
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGContextDrawImage
import platform.CoreGraphics.CGContextRelease
import platform.CoreGraphics.CGContextScaleCTM
import platform.CoreGraphics.CGContextTranslateCTM
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.CoreGraphics.kCGBitmapByteOrder32Big
import platform.CoreImage.*
import platform.Foundation.NSData
import platform.Foundation.setValue
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Foundation.dataWithContentsOfURL
import platform.UIKit.NSFontAttributeName
import platform.UIKit.NSForegroundColorAttributeName
import platform.UIKit.UIColor
import platform.UIKit.UIFont
import platform.UIKit.drawAtPoint
import platform.UIKit.sizeWithAttributes
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIRectFill
import platform.posix.memcpy

internal actual suspend fun renderTicketImageRaster(
    source: String,
    paperWidthDots: Int,
    widthHintPercent: Int?,
    alignment: PrintAlignment,
): ByteArray = withContext(Dispatchers.Default) {
    val image = loadTicketImage(source) ?: rasterFailure("No se pudo decodificar el logo del negocio")
    val scaled = image.scaledToPaper(paperWidthDots, widthHintPercent)
    val pixels = scaled.rgbaPixels() ?: rasterFailure("No se pudo leer el logo del negocio")
    pixelsToEscPosRaster(
        width = pixels.width,
        height = pixels.height,
        isBlack = { x, y -> pixels.isBlack(x, y) },
        paperWidthDots = paperWidthDots,
        alignment = alignment,
    )
}

internal actual fun renderTicketQrRaster(
    data: String,
    errorCorrection: String,
    paperWidthDots: Int,
    alignment: PrintAlignment,
): ByteArray {
    val level = when (errorCorrection.trim().uppercase()) {
        "L", "Q", "H" -> errorCorrection.trim().uppercase()
        else -> "M"
    }
    val message = NSString.create(string = data).dataUsingEncoding(NSUTF8StringEncoding)
        ?: rasterFailure("No se pudo generar el QR del ticket")
    val filter = CIFilter.filterWithName("CIQRCodeGenerator")
        ?: rasterFailure("No se pudo generar el QR del ticket")
    filter.setDefaults()
    filter.setValue(message, forKey = "inputMessage")
    filter.setValue(level, forKey = "inputCorrectionLevel")
    val ciImage = filter.outputImage ?: rasterFailure("No se pudo generar el QR del ticket")
    val context = CIContext.context()
    val cgImage = context.createCGImage(ciImage, fromRect = ciImage.extent)
        ?: rasterFailure("No se pudo generar el QR del ticket")
    val uiImage = UIImage.imageWithCGImage(cgImage)
    val pixels = uiImage.rgbaPixels() ?: rasterFailure("No se pudo generar el QR del ticket")
    return scaledModuleRaster(
        moduleWidth = pixels.width,
        moduleHeight = pixels.height,
        isDark = { x, y -> pixels.isBlack(x, y) },
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
    UIGraphicsBeginImageContextWithOptions(
        CGSizeMake(line.bitmapWidth.toDouble(), line.bitmapHeight.toDouble()),
        true,
        1.0,
    )
    val image = try {
        if (line.inverse) UIColor.blackColor.setFill() else UIColor.whiteColor.setFill()
        UIRectFill(CGRectMake(0.0, 0.0, line.bitmapWidth.toDouble(), line.bitmapHeight.toDouble()))
        if (line.glyphs.isNotEmpty()) {
            val font = receiptFont(line.textSize.toDouble(), line.bold)
            val ink = if (line.inverse) UIColor.whiteColor else UIColor.blackColor
            ink.setFill()
            line.glyphs.forEachIndexed { index, character ->
                if (character == ' ') return@forEachIndexed
                val glyph = NSString.create(string = character.toString())
                val attributes: Map<Any?, Any?> = mapOf(
                    NSFontAttributeName to font,
                    NSForegroundColorAttributeName to ink,
                )
                val measured = glyph.sizeWithAttributes(attributes)
                val glyphWidth = measured.useContents { width }
                val glyphHeight = measured.useContents { height }
                val x = line.originX.toDouble() +
                    index * line.cellWidth.toDouble() +
                    (line.cellWidth.toDouble() - glyphWidth) / 2.0
                val y = ((line.bitmapHeight.toDouble() - glyphHeight) / 2.0).coerceAtLeast(0.0)
                glyph.drawAtPoint(CGPointMake(x, y), withAttributes = attributes)
                if (line.underline) {
                    ink.setFill()
                    UIRectFill(
                        CGRectMake(
                            line.originX + index * line.cellWidth.toDouble(),
                            line.bitmapHeight - 2.0,
                            line.cellWidth.toDouble(),
                            1.0,
                        )
                    )
                }
            }
        }
        UIGraphicsGetImageFromCurrentImageContext()
    } finally {
        UIGraphicsEndImageContext()
    } ?: rasterFailure("No se pudo dibujar el texto del ticket")
    val pixels = image.rgbaPixels() ?: rasterFailure("No se pudo leer el texto del ticket")
    return pixelsToEscPosRaster(
        width = pixels.width,
        height = pixels.height,
        isBlack = { x, y -> pixels.isInk(x, y, if (line.inverse) 80 else 210) },
        paperWidthDots = pixels.width,
        alignment = PrintAlignment.LEFT,
        feedLine = false,
    )
}

private fun receiptFont(size: Double, bold: Boolean): UIFont {
    val name = if (bold) "Menlo-Bold" else "Menlo"
    return UIFont.fontWithName(name, size)
        ?: if (bold) UIFont.boldSystemFontOfSize(size) else UIFont.systemFontOfSize(size)
}

@OptIn(ExperimentalEncodingApi::class)
private fun loadTicketImage(source: String): UIImage? {
    val trimmed = source.trim()
    val data = if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
        val url = NSURL.URLWithString(trimmed) ?: return null
        NSData.dataWithContentsOfURL(url) ?: return null
    } else {
        val bytes = try {
            Base64.decode(trimmed.substringAfter("base64,", trimmed))
        } catch (_: Exception) {
            return null
        }
        bytes.toNsData()
    }
    return UIImage.imageWithData(data)
}

private fun UIImage.scaledToPaper(paperWidthDots: Int, widthHintPercent: Int?): UIImage {
    val sourceWidth = size.useContents { width }.coerceAtLeast(1.0)
    val sourceHeight = size.useContents { height }.coerceAtLeast(1.0)
    val maxWidth = paperWidthDots.coerceAtLeast(1)
    val requested = widthHintPercent
        ?.coerceIn(1, 100)
        ?.let { (maxWidth * it / 100.0).roundToInt() }
        ?: sourceWidth.roundToInt()
    val targetWidth = requested.coerceIn(1, maxWidth).toDouble()
    val targetHeight = (sourceHeight * (targetWidth / sourceWidth)).roundToInt().coerceIn(1, 800).toDouble()
    UIGraphicsBeginImageContextWithOptions(CGSizeMake(targetWidth, targetHeight), true, 1.0)
    return try {
        UIColor.whiteColor.setFill()
        UIRectFill(CGRectMake(0.0, 0.0, targetWidth, targetHeight))
        drawInRect(CGRectMake(0.0, 0.0, targetWidth, targetHeight))
        UIGraphicsGetImageFromCurrentImageContext() ?: this
    } finally {
        UIGraphicsEndImageContext()
    }
}

private class RgbaPixels(
    val width: Int,
    val height: Int,
    private val bytes: ByteArray,
) {
    fun isBlack(x: Int, y: Int): Boolean = isInk(x, y, 180)

    fun isInk(x: Int, y: Int, threshold: Int): Boolean {
        val index = (y * width + x) * 4
        val red = bytes[index].toInt() and 0xFF
        val green = bytes[index + 1].toInt() and 0xFF
        val blue = bytes[index + 2].toInt() and 0xFF
        val alpha = bytes[index + 3].toInt() and 0xFF
        if (alpha < 40) return false
        return (red * 30 + green * 59 + blue * 11) / 100 < threshold
    }
}

private fun UIImage.rgbaPixels(): RgbaPixels? {
    val cgImage = CGImage ?: return null
    val width = CGImageGetWidth(cgImage).toInt()
    val height = CGImageGetHeight(cgImage).toInt()
    if (width <= 0 || height <= 0) return null
    val bytesPerRow = width * 4
    val buffer = ByteArray(bytesPerRow * height)
    val colorSpace = CGColorSpaceCreateDeviceRGB() ?: return null
    val drawn = try {
        buffer.usePinned { pinned ->
            val context = CGBitmapContextCreate(
                data = pinned.addressOf(0),
                width = width.toULong(),
                height = height.toULong(),
                bitsPerComponent = 8uL,
                bytesPerRow = bytesPerRow.toULong(),
                space = colorSpace,
                bitmapInfo = CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value or kCGBitmapByteOrder32Big,
            ) ?: return@usePinned false
            try {
                CGContextTranslateCTM(context, 0.0, height.toDouble())
                CGContextScaleCTM(context, 1.0, -1.0)
                CGContextDrawImage(
                    context,
                    CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble()),
                    cgImage,
                )
                true
            } finally {
                CGContextRelease(context)
            }
        }
    } finally {
        CGColorSpaceRelease(colorSpace)
    }
    if (!drawn) return null
    return RgbaPixels(width, height, buffer)
}

private fun ByteArray.toNsData(): NSData =
    usePinned { pinned -> NSData.create(bytes = pinned.addressOf(0), length = size.toULong()) }
