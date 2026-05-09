package com.teco.ventago.core

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File


class AndroidPdfSharer(
    private val context: Context
) : PdfSharer {

    override fun sharePdf(filename: String, bytes: ByteArray) {
        val cacheDir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(cacheDir, filename)
        file.writeBytes(bytes)

        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.provider", file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(filename, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val resInfoList = context.packageManager.queryIntentActivities(intent, 0)
        for (resInfo in resInfoList) {
            context.grantUriPermission(
                resInfo.activityInfo.packageName,
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        context.startActivity(
            Intent.createChooser(intent, "Compartir factura PDF")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    override fun openPdf(filename: String, bytes: ByteArray) {
        val cacheDir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(cacheDir, filename).apply { writeBytes(bytes) }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            // IMPORTANT: set both data and type
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        }

        // Grant URI read permission to all potential receivers (some viewers need this)
        val resInfoList = context.packageManager.queryIntentActivities(viewIntent, 0)
        for (resInfo in resInfoList) {
            val pkgName = resInfo.activityInfo.packageName
            context.grantUriPermission(
                pkgName,
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

        try {
            context.startActivity(
                Intent.createChooser(viewIntent, "Abrir PDF")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: ActivityNotFoundException) {
            // No PDF viewer installed → fall back to share as a workaround
            sharePdf(filename, bytes)
        }
    }

    override fun saveImageToGallery(filename: String, bytes: ByteArray, mimeType: String): Boolean {
        return saveToMediaStore(
            collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            relativePath = "${Environment.DIRECTORY_PICTURES}/VentaGo",
            filename = filename,
            mimeType = mimeType,
            bytes = bytes
        )
    }

    override fun saveFileToDocuments(filename: String, bytes: ByteArray, mimeType: String): Boolean {
        return saveToMediaStore(
            collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            relativePath = "${Environment.DIRECTORY_DOWNLOADS}/VentaGo",
            filename = filename,
            mimeType = mimeType,
            bytes = bytes
        )
    }

    private fun saveToMediaStore(
        collection: android.net.Uri,
        relativePath: String,
        filename: String,
        mimeType: String,
        bytes: ByteArray
    ): Boolean {
        return runCatching {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val uri = resolver.insert(collection, values) ?: return false
            resolver.openOutputStream(uri)?.use { output ->
                output.write(bytes)
            } ?: run {
                resolver.delete(uri, null, null)
                return false
            }

            val publishValues = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            resolver.update(uri, publishValues, null, null)
            true
        }.getOrElse { false }
    }
}
