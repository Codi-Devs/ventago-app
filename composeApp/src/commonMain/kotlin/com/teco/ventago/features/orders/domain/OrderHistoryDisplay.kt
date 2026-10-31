package com.teco.ventago.features.orders.domain

import com.teco.ventago.features.orders.domain.models.OrderHistoryDto

private const val PENDING_SYNC_ACTOR = "pending-sync-job"
private const val INVOICE_HISTORY = 1
private const val INVOICE_ISSUED = 2

fun visibleOrderHistories(histories: List<OrderHistoryDto>): List<OrderHistoryDto> {
    val personAlreadyIssued = histories.any(::isPersonInvoiceIssued)
    return histories.mapNotNull { history ->
        if (!isPendingSync(history)) {
            return@mapNotNull history
        }
        if (isSyncSuccess(history) && personAlreadyIssued) {
            return@mapNotNull null
        }
        history.copy(
            changedBy = "Sistema",
            note = friendlySyncNote(history.note),
        )
    }
}

private fun isPendingSync(history: OrderHistoryDto): Boolean {
    return history.changedBy.trim() == PENDING_SYNC_ACTOR
}

private fun isSyncSuccess(history: OrderHistoryDto): Boolean {
    return history.note.contains("sync pendiente")
}

private fun isPersonInvoiceIssued(history: OrderHistoryDto): Boolean {
    if (isPendingSync(history)) {
        return false
    }
    if (history.historyType == INVOICE_HISTORY && history.statusId == INVOICE_ISSUED) {
        return true
    }
    return history.note.contains("Factura emitida") && !history.note.contains("sync pendiente")
}

private fun friendlySyncNote(note: String): String {
    return when {
        note.contains("sync pendiente") -> "Factura confirmada por el sistema"
        note.contains("Facturación fallida") || note.contains("Identidad fiscal") ->
            "No se pudo confirmar la factura. Puedes reintentarla."
        else -> note
    }
}
