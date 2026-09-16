package com.teco.ventago.features.expenses.ui.upload

import androidx.compose.runtime.Composable

@Composable
expect fun InvoiceDocumentCamera(
    onCaptured: (ByteArray) -> Unit,
    onClose: () -> Unit
)
