package com.teco.ventago.features.expenses.domain

object CrawlErrorCopy {
    const val DEFAULT = "No se pudo importar esta factura desde DGI."
    const val TIMEOUT = "La DGI no respondió a tiempo. Intente de nuevo."
    const val NOT_FOUND = "La DGI no encontró esta factura."
    const val INVALID = "El CUFE no es válido."
    const val DUPLICATE = "Esta factura ya está registrada."

    fun userMessage(raw: String?): String {
        val text = raw?.trim().orEmpty()
        if (text.isBlank()) return DEFAULT

        val lower = text.lowercase()
        if (isAlreadyUserFacing(text, lower)) return text
        if (!looksTechnical(lower)) return text

        return when {
            lower.contains("timeout") ||
                lower.contains("deadline") ||
                lower.contains("timed out") -> TIMEOUT
            lower.contains("not found") || lower.contains("404") -> NOT_FOUND
            lower.contains("invalid") && lower.contains("cufe") -> INVALID
            lower.contains("already") ||
                lower.contains("duplicate") ||
                lower.contains("exist") -> DUPLICATE
            else -> DEFAULT
        }
    }

    private fun isAlreadyUserFacing(text: String, lower: String): Boolean {
        if (text.any { it in "áéíóúñÁÉÍÓÚÑ" }) return true
        return lower.contains("factura") ||
            lower.contains("no se pudo") ||
            lower.contains("intente de nuevo")
    }

    private fun looksTechnical(lower: String): Boolean {
        return lower.contains("panic") ||
            lower.contains("goroutine") ||
            lower.contains("http") ||
            lower.contains("status") ||
            lower.contains("timeout") ||
            lower.contains("deadline") ||
            lower.contains("connection") ||
            lower.contains("eof") ||
            lower.contains("sql") ||
            lower.contains("json") ||
            lower.contains("stack") ||
            Regex("\\b[45]\\d{2}\\b").containsMatchIn(lower)
    }
}
