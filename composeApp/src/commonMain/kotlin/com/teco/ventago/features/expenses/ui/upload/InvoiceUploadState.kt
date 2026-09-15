package com.teco.ventago.features.expenses.ui.upload

data class InvoiceUploadState(
    val hasInvoiceUploadAccess: Boolean = false,
    val isScanning: Boolean = false,
    val isUploading: Boolean = false,
    val selectedFileName: String? = null,
    val showAccepted: Boolean = false,
    val error: String? = null
)
