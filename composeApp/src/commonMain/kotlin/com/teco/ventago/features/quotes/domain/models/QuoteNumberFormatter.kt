package com.teco.ventago.features.quotes.domain.models

fun formatQuoteDisplayNumber(number: String?): String {
    val value = number?.trim()?.removePrefix("#").orEmpty()
    if (value.isBlank()) return ""

    val parts = value.split("-").filter { it.isNotBlank() }
    return if (parts.size >= 4) {
        parts.takeLast(2).joinToString("-")
    } else {
        value
    }
}

fun Quote.compactDisplayNumber(): String = formatQuoteDisplayNumber(displayNumber ?: quoteNumber)
