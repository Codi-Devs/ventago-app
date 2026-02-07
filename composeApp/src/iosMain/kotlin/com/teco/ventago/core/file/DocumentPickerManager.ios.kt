package com.teco.ventago.core.file

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberDocumentPickerManager(
    acceptedMimeTypes: List<String>,
    onResult: (SharedFile?) -> Unit
): DocumentPickerManager {
    return remember {
        DocumentPickerManager(onLaunch = { onResult(null) })
    }
}

actual class DocumentPickerManager actual constructor(private val onLaunch: () -> Unit) {
    actual fun launch() {
        onLaunch()
    }
}
