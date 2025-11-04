package com.teco.ventago.features.invoicing.domain

enum class TaxPayerType(val code: String, val description: String) {
    NATURAL("1", "Persona Natural"),
    JURIDICAL("2", "Persona Jurídica"),
}