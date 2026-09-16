package com.teco.ventago.core.camera

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream

actual class SharedImage(
    private val bitmap: android.graphics.Bitmap?,
    private val encodedBytes: ByteArray? = null
) {
    actual fun toByteArray(): ByteArray? {
        encodedBytes?.let { return it }
        return if (bitmap != null) {
            val byteArrayOutputStream = ByteArrayOutputStream()
            @Suppress("MagicNumber") bitmap.compress(
                android.graphics.Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream
            )
            byteArrayOutputStream.toByteArray()
        } else {
            println("toByteArray null")
            null
        }
    }

    actual fun toImageBitmap(): ImageBitmap? {
        encodedBytes?.let { bytes ->
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }
        val byteArray = toByteArray()
        return if (byteArray != null) {
            return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size).asImageBitmap()
        } else {
            println("toImageBitmap null")
            null
        }
    }
}
