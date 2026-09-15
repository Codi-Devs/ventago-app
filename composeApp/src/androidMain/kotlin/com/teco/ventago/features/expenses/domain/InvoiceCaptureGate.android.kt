package com.teco.ventago.features.expenses.domain

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal actual fun decodeInvoiceRaster(bytes: ByteArray, maxSide: Int): InvoiceRaster? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val sample = sampleSize(bounds.outWidth, bounds.outHeight, maxSide)
    val bitmap = BitmapFactory.decodeByteArray(
        bytes,
        0,
        bytes.size,
        BitmapFactory.Options().apply { inSampleSize = sample }
    ) ?: return null
    return try {
        toRaster(bitmap, bounds.outWidth, bounds.outHeight)
    } finally {
        if (!bitmap.isRecycled) bitmap.recycle()
    }
}

internal actual suspend fun scanQrFromImageBytes(bytes: ByteArray): String? =
    withContext(Dispatchers.Default) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
        val sample = sampleSize(bounds.outWidth, bounds.outHeight, 1600)
        val bitmap = BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sample }
        ) ?: return@withContext null
        try {
            decodeQr(bitmap)
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

private fun sampleSize(width: Int, height: Int, maxSide: Int): Int {
    var sample = 1
    val longest = maxOf(width, height)
    while (longest / sample > maxSide * 2) {
        sample *= 2
    }
    return sample
}

private fun toRaster(bitmap: Bitmap, sourceWidth: Int, sourceHeight: Int): InvoiceRaster {
    val argb = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(argb, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    return InvoiceImageQuality.downsampleArgb(argb, bitmap.width, bitmap.height).copy(
        sourceWidth = sourceWidth,
        sourceHeight = sourceHeight
    )
}

private fun decodeQr(bitmap: Bitmap): String? {
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    val source = RGBLuminanceSource(width, height, pixels)
    val reader = MultiFormatReader()
    reader.setHints(
        mapOf(
            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
            DecodeHintType.TRY_HARDER to true
        )
    )
    return runCatching {
        reader.decodeWithState(BinaryBitmap(HybridBinarizer(source))).text
    }.getOrNull()
}
