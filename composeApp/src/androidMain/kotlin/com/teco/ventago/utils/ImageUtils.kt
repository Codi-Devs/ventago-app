package com.teco.ventago.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.content.FileProvider
import coil3.PlatformContext
import coil3.request.ImageRequest
import coil3.request.allowHardware
import org.koin.java.KoinJavaComponent
import java.io.File
import java.io.OutputStream

class AndroidImageSaver(private val context: Context) : ImageSaver {
    override fun saveImage(image: Any, fileName: String): String? {

        val imageBitmap = image as ImageBitmap
        val bitmap = imageBitmap.asAndroidBitmap()
        // Cast the generic image to Bitmap

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName) // File name with extension
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")      // MIME type
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MyAppImages") // Directory inside Pictures
            put(MediaStore.Images.Media.IS_PENDING, 1)               // Pending state while saving
        }

        val resolver = context.contentResolver
        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        if (imageUri != null) {
            var outputStream: OutputStream? = null
            try {
                outputStream = resolver.openOutputStream(imageUri)
                if (outputStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0) // Mark as saved
                    resolver.update(imageUri, contentValues, null, null)

                    val externalDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    val appDir = File(externalDir, "MyAppImages")
                    if (!appDir.exists()) appDir.mkdirs()

                    return File(appDir, fileName).absolutePath
                } else {
                    Toast.makeText(context, "Failed to create MediaStore entry", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(context, "Error saving image: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                outputStream?.close()
            }
        } else {
            Toast.makeText(context, "Failed to create MediaStore entry", Toast.LENGTH_SHORT).show()
        }
        return null
    }

}

actual fun createImageSaver(): ImageSaver {
    val context: Context = KoinJavaComponent.getKoin().get()
    return AndroidImageSaver(context)
}

actual fun openFileInGallery(filePath: String) {
    val context: Context = KoinJavaComponent.getKoin().get()
    val file = File(filePath)

    // Get the URI for the file using FileProvider
    val uri: Uri = FileProvider.getUriForFile(
        context,
        context.packageName, // Replace with your provider authority
        file
    )

    // Create and launch an intent to view the file
    val intent = Intent().apply {
        action = Intent.ACTION_VIEW
        setDataAndType(uri, "image/*")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
    }

    context.startActivity( intent)
}

actual fun shareInvoice(path: String) {
    val context: Context = KoinJavaComponent.getKoin().get()

    val imageUri = FileProvider.getUriForFile(
        context, context.packageName, File(path)
    )
    val share = Intent(Intent.ACTION_SEND)
    share.setType("image/png")
    share.putExtra(Intent.EXTRA_STREAM, imageUri)
    share.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    val intent = Intent.createChooser(share, "Share Via")
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

internal actual fun getImageRequest(context: PlatformContext, url: String): ImageRequest {
    return ImageRequest.Builder(context)
        .data(url)
        .allowHardware(false)
        .build()
}
