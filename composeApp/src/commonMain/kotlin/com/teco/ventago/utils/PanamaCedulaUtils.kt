package com.teco.ventago.utils

private val PANAMA_CEDULA_PATTERNS = listOf(
    Regex("^[1-9]\\d?-\\d{2,4}-\\d{2,5}$"),
    Regex("^PE-\\d{3,4}-\\d{5}$"),
    Regex("^E-\\d{4}-\\d{5,6}$"),
    Regex("^N-\\d{4,5}-\\d{4}$"),
    Regex("^[1-9]\\d?AV-?\\d{4}-\\d{5}$"),
    Regex("^[1-9]\\d?PI-\\d{4}-\\d{4,5}$"),
)

fun normalizePanamaCedula(input: String): String {
    return input.trim().uppercase()
}

fun isValidPanamaCedula(input: String): Boolean {
    val normalizedInput = normalizePanamaCedula(input)
    return PANAMA_CEDULA_PATTERNS.any { pattern -> pattern.matches(normalizedInput) }
}
