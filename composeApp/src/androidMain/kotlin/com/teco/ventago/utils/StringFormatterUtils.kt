package com.teco.ventago.utils

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale


actual fun formatNumberToMoney(amount: String): String {
    val doubleAmount = amount.toDoubleOrNull() ?: 0.0
    val format: NumberFormat = NumberFormat.getCurrencyInstance()

    val locale = Locale.getDefault()
    val country = locale.country
    val currency = try {
        if (country.isNullOrEmpty()) {
            Currency.getInstance(Locale("en", "US")) // Default fallback
        } else {
            Currency.getInstance(locale)
        }
    } catch (e: Exception) {
        Currency.getInstance(Locale("en", "US")) // Safe fallback
    }

    format.maximumFractionDigits = 2
    format.currency = currency

    return format.format(doubleAmount)
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