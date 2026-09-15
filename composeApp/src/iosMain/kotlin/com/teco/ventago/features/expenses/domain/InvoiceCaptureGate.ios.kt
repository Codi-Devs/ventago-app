package com.teco.ventago.features.expenses.domain

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage
import platform.Vision.VNBarcodeObservation
import platform.Vision.VNBarcodeSymbologyQR
import platform.Vision.VNDetectBarcodesRequest
import platform.Vision.VNImageRequestHandler

@OptIn(ExperimentalForeignApi::class)
internal actual fun decodeInvoiceRaster(bytes: ByteArray, maxSide: Int): InvoiceRaster? {
    val image = runCatching { Image.makeFromEncoded(bytes) }.getOrNull() ?: return null
    val width = image.width
    val height = image.height
    if (width <= 0 || height <= 0) return null
    val pixmap = image.peekPixels() ?: return null
    val scale = minOf(1f, maxSide.toFloat() / maxOf(width, height).toFloat())
    val outW = maxOf(1, kotlin.math.round(width * scale).toInt())
    val outH = maxOf(1, kotlin.math.round(height * scale).toInt())
    val argb = IntArray(outW * outH)
    for (y in 0 until outH) {
        val srcY = minOf(height - 1, (y / scale).toInt())
        for (x in 0 until outW) {
            val srcX = minOf(width - 1, (x / scale).toInt())
            argb[y * outW + x] = pixmap.getColor(srcX, srcY)
        }
    }
    return InvoiceImageQuality.downsampleArgb(argb, outW, outH).copy(
        sourceWidth = width,
        sourceHeight = height
    )
}

@OptIn(ExperimentalForeignApi::class)
internal actual suspend fun scanQrFromImageBytes(bytes: ByteArray): String? =
    withContext(Dispatchers.Default) {
        val data = bytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
        }
        val uiImage = UIImage.imageWithData(data) ?: return@withContext null
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
        val handler = VNImageRequestHandler(cgImage = cgImage, options = emptyMap<Any?, Any?>())
        runCatching { handler.performRequests(listOf(request), error = null) }
        payload
    }
