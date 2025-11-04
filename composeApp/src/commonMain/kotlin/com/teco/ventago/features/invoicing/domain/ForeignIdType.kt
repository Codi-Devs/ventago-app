package com.teco.ventago.features.invoicing.domain

enum class ForeignIdType (val code: String, val description: String) {
    PASSPORT("01", "Pasaporte"),
    FOREIGN_TAX_ID("02", "Número de identificación tributaria extranjera")
}