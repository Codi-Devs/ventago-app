package com.teco.ventago.features.expenses.domain

object CufeParser {
    private val queryCufe = Regex("[?&]chFE=([^&]+)", RegexOption.IGNORE_CASE)
    private val pathCufe = Regex(
        "/FacturasPor(?:CUFE|QR)/(FE[^/\\s?]+)",
        RegexOption.IGNORE_CASE
    )

    fun parse(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        queryCufe.find(trimmed)?.groupValues?.getOrNull(1)?.let { raw ->
            normalize(percentDecode(raw))?.let { return it }
        }
        pathCufe.find(trimmed)?.groupValues?.getOrNull(1)?.let { raw ->
            normalize(percentDecode(raw))?.let { return it }
        }
        return normalize(trimmed)
    }

    fun truncate(cufe: String, head: Int = 12, tail: Int = 8): String {
        val value = cufe.trim()
        if (value.length <= head + tail + 1) return value
        return value.take(head) + "…" + value.takeLast(tail)
    }

    private fun normalize(value: String): String? {
        val cufe = value.trim()
        if (cufe.startsWith("FE") && cufe.length >= 50) return cufe
        return null
    }

    internal fun percentDecode(value: String): String {
        if ('%' !in value && '+' !in value) return value
        val out = StringBuilder(value.length)
        var index = 0
        while (index < value.length) {
            val current = value[index]
            if (current == '%' && index + 2 < value.length) {
                val decoded = value.substring(index + 1, index + 3).toIntOrNull(16)
                if (decoded != null) {
                    out.append(decoded.toChar())
                    index += 3
                    continue
                }
            }
            out.append(if (current == '+') ' ' else current)
            index++
        }
        return out.toString()
    }
}
