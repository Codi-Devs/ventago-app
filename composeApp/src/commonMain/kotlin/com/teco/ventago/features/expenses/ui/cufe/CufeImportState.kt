package com.teco.ventago.features.expenses.ui.cufe

import com.teco.ventago.features.expenses.domain.models.CrawlJob

data class CufeImportState(
    val cufeInput: String = "",
    val isImporting: Boolean = false,
    val currentJob: CrawlJob? = null,
    val isPolling: Boolean = false,
    val pollingTimedOut: Boolean = false,
    val importSuccess: Boolean = false,
    val importedExpenseId: Long? = null,
    val isOpeningExpense: Boolean = false,
    val error: String? = null,
    val showScanner: Boolean = false,
    val showPermissionDialog: Boolean = false
) {
    val jobStatusLabel: String?
        get() {
            if (pollingTimedOut) {
                return "No se pudo confirmar la importación en 1 minuto."
            }
            return when (currentJob?.status) {
                "pending" -> "En cola..."
                "processing" -> "Procesando factura..."
                "success", "completed" -> "Factura importada exitosamente"
                "failed", "error", "cancelled", "timeout" ->
                    currentJob.errorMessage ?: currentJob.message ?: "Error al importar"
                else -> currentJob?.message
            }
        }
}
