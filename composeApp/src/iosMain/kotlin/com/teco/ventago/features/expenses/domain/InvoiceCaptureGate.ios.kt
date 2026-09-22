@file:OptIn(ExperimentalForeignApi::class)

package com.teco.ventago.features.expenses.domain

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.get
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGContextDrawImage
import platform.CoreGraphics.CGContextRelease
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.kCGBitmapByteOrder32Big
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.Vision.VNBarcodeObservation
import platform.Vision.VNBarcodeSymbologyQR
import platform.Vision.VNDetectBarcodesRequest
import platform.Vision.VNImageRequestHandler

internal actual fun decodeInvoiceRaster(bytes: ByteArray, maxSide: Int): InvoiceRaster? {
    val uiImage = bytes.toUIImage() ?: return null
    val cgImage = uiImage.CGImage ?: return null
    val sourceWidth = CGImageGetWidth(cgImage).toInt()
    val sourceHeight = CGImageGetHeight(cgImage).toInt()
    if (sourceWidth <= 0 || sourceHeight <= 0) return null

    val scale = minOf(1f, maxSide.toFloat() / maxOf(sourceWidth, sourceHeight).toFloat())
    val outW = maxOf(1, kotlin.math.round(sourceWidth * scale).toInt())
    val outH = maxOf(1, kotlin.math.round(sourceHeight * scale).toInt())
    val bytesPerRow = outW * 4
    val buffer = ByteArray(bytesPerRow * outH)
    val colorSpace = CGColorSpaceCreateDeviceRGB() ?: return null
    val drawn = try {
        buffer.usePinned { pinned ->
            val context = CGBitmapContextCreate(
                data = pinned.addressOf(0),
                width = outW.toULong(),
                height = outH.toULong(),
                bitsPerComponent = 8uL,
                bytesPerRow = bytesPerRow.toULong(),
                space = colorSpace,
                bitmapInfo = CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value or
                    kCGBitmapByteOrder32Big
            ) ?: return@usePinned false
            try {
                CGContextDrawImage(
                    context,
                    CGRectMake(0.0, 0.0, outW.toDouble(), outH.toDouble()),
                    cgImage
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

    val argb = IntArray(outW * outH)
    var index = 0
    for (y in 0 until outH) {
        var row = y * bytesPerRow
        for (x in 0 until outW) {
            val r = buffer[row].toInt() and 0xFF
            val g = buffer[row + 1].toInt() and 0xFF
            val b = buffer[row + 2].toInt() and 0xFF
            argb[index++] = (r shl 16) or (g shl 8) or b
            row += 4
        }
    }
    return InvoiceImageQuality.downsampleArgb(argb, outW, outH).copy(
        sourceWidth = sourceWidth,
        sourceHeight = sourceHeight
    )
}

internal actual suspend fun scanQrFromImageBytes(bytes: ByteArray): String? =
    withContext(Dispatchers.Default) {
        val uiImage = bytes.toUIImage() ?: return@withContext null
        val cgImage = uiImage.CGImage ?: return@withContext null
        var payload: String? = null
        val request = VNDetectBarcodesRequest { req, _ ->
            val results = req?.results.orEmpty()
            for (item in results) {
                val observation = item as? VNBarcodeObservation ?: continue
                val value = observation.payloadStringValue
                if (!value.isNullOrBlank()) {
                    payload = value
                    break
                }
            }
        }
        request.symbologies = listOf(VNBarcodeSymbologyQR)
        val handler = VNImageRequestHandler(cGImage = cgImage, options = emptyMap<Any?, Any?>())
        runCatching { handler.performRequests(listOf(request), error = null) }
        payload
    }

internal actual fun fitInvoiceJpegUnderLimit(bytes: ByteArray, maxBytes: Int): ByteArray {
    if (bytes.size <= maxBytes) return bytes
    val image = bytes.toUIImage() ?: return bytes
    var quality = 0.92
    repeat(8) {
        val jpeg = UIImageJPEGRepresentation(image, quality) ?: return bytes
        val length = jpeg.length.toInt()
        if (length <= maxBytes) {
            val pointer = jpeg.bytes?.reinterpret<ByteVar>() ?: return bytes
            return ByteArray(length) { index -> pointer[index] }
        }
        quality = maxOf(0.7, quality - 0.08)
    }
    return bytes
}

private fun ByteArray.toUIImage(): UIImage? {
    val data = usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
    return UIImage.imageWithData(data)
}
