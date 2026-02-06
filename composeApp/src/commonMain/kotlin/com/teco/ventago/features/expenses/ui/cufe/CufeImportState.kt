package com.teco.ventago.features.expenses.ui.cufe

import com.teco.ventago.features.expenses.domain.models.CrawlJob

data class CufeImportState(
    val cufeInput: String = "",
    val isImporting: Boolean = false,
    val currentJob: CrawlJob? = null,
    val isPolling: Boolean = false,
    val importSuccess: Boolean = false,
    val importedExpenseId: Long? = null,
    val error: String? = null,
    val showScanner: Boolean = false,
    val showPermissionDialog: Boolean = false
) {
    val jobStatusLabel: String?
        get() = when (currentJob?.status) {
            "pending" -> "En cola..."
            "processing" -> "Procesando factura..."
            "success" -> "Factura importada exitosamente"
            "failed" -> currentJob.errorMessage ?: "Error al importar"
            else -> null
        }
}
