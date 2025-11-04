package com.teco.ventago.core

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
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

        val intent = Intent(Intent.ACTION_VIEW).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
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
}