package com.teco.ventago.core.file

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberDocumentPickerManager(
    acceptedMimeTypes: List<String>,
    onResult: (SharedFile?) -> Unit
): DocumentPickerManager {
    val context = LocalContext.current
    val contentResolver = context.contentResolver
    val mimeTypes = acceptedMimeTypes.ifEmpty { listOf("*/*") }

    val pickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) {
                onResult(null)
                return@rememberLauncherForActivityResult
            }

            val file = readSharedFile(contentResolver, uri, mimeTypes)
            onResult(file)
        }

    return remember(mimeTypes) {
        DocumentPickerManager(onLaunch = { pickerLauncher.launch(mimeTypes.toTypedArray()) })
    }
}

private fun readSharedFile(
    contentResolver: ContentResolver,
    uri: Uri,
    acceptedMimeTypes: List<String>
): SharedFile? {
    val fileName = resolveFileName(contentResolver, uri) ?: "archivo"
    val resolvedMimeType = resolveMimeType(contentResolver, uri, fileName)

    if (!isAllowedMimeType(resolvedMimeType, acceptedMimeTypes)) {
        return null
    }

    val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null

    return SharedFile(
        bytes = bytes,
        fileName = fileName,
        contentType = resolvedMimeType
    )
}

private fun resolveFileName(contentResolver: ContentResolver, uri: Uri): String? {
    val cursor = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) {
                return it.getString(index)
            }
        }
    }
    return null
}

private fun resolveMimeType(contentResolver: ContentResolver, uri: Uri, fileName: String): String {
    val fromResolver = contentResolver.getType(uri)
    if (!fromResolver.isNullOrBlank()) return fromResolver

    val extension = fileName.substringAfterLast('.', "").lowercase()
    return when (extension) {
        "pdf" -> "application/pdf"
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "webp" -> "image/webp"
        else -> "application/octet-stream"
    }
}

private fun isAllowedMimeType(mimeType: String, acceptedMimeTypes: List<String>): Boolean {
    if (acceptedMimeTypes.isEmpty()) return true
    if (acceptedMimeTypes.any { it == "*/*" }) return true
    return acceptedMimeTypes.any { accepted ->
        if (accepted.endsWith("/*")) {
            val prefix = accepted.substringBefore("/*")
            mimeType.startsWith("$prefix/")
        } else {
            mimeType.equals(accepted, ignoreCase = true)
        }
    }
}

actual class DocumentPickerManager actual constructor(private val onLaunch: () -> Unit) {
    actual fun launch() {
        onLaunch()
    }
}
