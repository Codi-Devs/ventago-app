package com.teco.ventago.features.quotes.domain.models

object QuoteStatus {
    const val DRAFT = 0
    const val CREATED = 1
    const val ACCEPTED = 2
    const val REJECTED = 3
    const val CANCELLED = 4

    fun label(status: Int?): String = when (status) {
        DRAFT -> "Borrador"
        CREATED -> "Creada"
        ACCEPTED -> "Aceptada"
        REJECTED -> "Rechazada"
        CANCELLED -> "Cancelada"
        else -> "Desconocido"
    }
}
