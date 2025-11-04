package com.teco.ventago.utils

actual fun formatNumberToMoney(amount: String): String {
    return ""
}

actual fun escapeJsonString(value: String): String {
    return buildString {
        for (char in value) {
            when (char) {
                '\"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(char)
            }
        }
    }
}