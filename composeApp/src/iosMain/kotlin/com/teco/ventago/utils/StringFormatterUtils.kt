package com.teco.ventago.utils

import platform.Foundation.NSLocale
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterCurrencyStyle
import platform.Foundation.currentLocale

actual fun formatNumberToMoney(amount: String): String {
    val numericAmount = amount.trim().toDoubleOrNull() ?: 0.0
    val formatter = NSNumberFormatter().apply {
        numberStyle = NSNumberFormatterCurrencyStyle
        locale = NSLocale.currentLocale
        minimumFractionDigits = 2u
        maximumFractionDigits = 2u
    }

    return formatter.stringFromNumber(NSNumber(double = numericAmount)) ?: "$${numericAmount}"
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
