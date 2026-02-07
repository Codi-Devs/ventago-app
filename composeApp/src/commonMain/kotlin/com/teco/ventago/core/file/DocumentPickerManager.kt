package com.teco.ventago.core.file

import androidx.compose.runtime.Composable

@Composable
expect fun rememberDocumentPickerManager(
    acceptedMimeTypes: List<String> = listOf("application/pdf"),
    onResult: (SharedFile?) -> Unit
): DocumentPickerManager

expect class DocumentPickerManager(
    onLaunch: () -> Unit
) {
    fun launch()
}
