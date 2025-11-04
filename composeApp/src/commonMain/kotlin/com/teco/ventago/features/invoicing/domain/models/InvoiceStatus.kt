package com.teco.ventago.features.invoicing.domain.models

enum class InvoiceStatus(val id: Int, val description: String) {
    NONE(0, "Sin facturacion"),
    PENDING(1, "Pendiente"),
    ISSUED(2, "Emitida"),
    FAILED( 3, "Fallida"),
    CANCELLED(4, "Anulada");

    companion object {
        fun fromId(id: Int): InvoiceStatus {
            return entries.find { it.id == id } ?: NONE
        }
    }
}