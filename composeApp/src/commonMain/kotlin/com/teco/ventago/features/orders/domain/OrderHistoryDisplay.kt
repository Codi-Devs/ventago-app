package com.teco.ventago.features.orders.domain

import com.teco.ventago.features.orders.domain.models.OrderHistoryDto

private const val PENDING_SYNC_ACTOR = "pending-sync-job"
private const val INVOICE_HISTORY = 1
private const val INVOICE_ISSUED = 2
private const val INVOICE_FAILED = 3

fun visibleOrderHistories(histories: List<OrderHistoryDto>): List<OrderHistoryDto> {
    val personAlreadyIssued = histories.any(::isPersonInvoiceIssued)
    return histories.mapNotNull { history ->
        if (!isPendingSync(history)) {
            return@mapNotNull history
        }
        if (isInvoiceIssued(history) && personAlreadyIssued) {
            return@mapNotNull null
        }
        history.copy(
            changedBy = "Sistema",
            note = friendlySyncNote(history, histories),
        )
    }
}

private fun isPendingSync(history: OrderHistoryDto): Boolean {
    return history.changedBy.trim() == PENDING_SYNC_ACTOR
}

private fun isInvoiceIssued(history: OrderHistoryDto): Boolean {
    return history.historyType == INVOICE_HISTORY && history.statusId == INVOICE_ISSUED
}

private fun isInvoiceFailed(history: OrderHistoryDto): Boolean {
    return (history.historyType == INVOICE_HISTORY && history.statusId == INVOICE_FAILED) ||
        history.note.contains("Facturación fallida") ||
        history.note.contains("Identidad fiscal")
}

private fun isPersonInvoiceIssued(history: OrderHistoryDto): Boolean {
    return !isPendingSync(history) && isInvoiceIssued(history)
}

private fun hasIssuedInvoiceAfter(histories: List<OrderHistoryDto>, failure: OrderHistoryDto): Boolean {
    val failureAt = failure.createdAt.trim()
    return histories.any { row ->
        if (row == failure || !isInvoiceIssued(row)) {
            return@any false
        }
        val issuedAt = row.createdAt.trim()
        if (failureAt.isEmpty() || issuedAt.isEmpty()) {
            return@any true
        }
        issuedAt >= failureAt
    }
}

private fun friendlySyncNote(history: OrderHistoryDto, histories: List<OrderHistoryDto>): String {
    if (isInvoiceIssued(history)) {
        return "Factura confirmada por el sistema"
    }
    if (isInvoiceFailed(history)) {
        if (hasIssuedInvoiceAfter(histories, history)) {
            return "No se pudo confirmar la factura"
        }
        return "No se pudo confirmar la factura. Puedes reintentarla."
    }
    return history.note
}
