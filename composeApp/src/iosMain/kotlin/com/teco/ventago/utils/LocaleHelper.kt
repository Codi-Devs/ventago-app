package com.teco.ventago.utils

import platform.Foundation.NSLocale
import platform.Foundation.NSNumberFormatter
import platform.Foundation.currentLocale
import platform.Foundation.languageCode

actual object LocaleHelper {

    actual fun getLocale(): String {
        return try {
            val myLang: String = NSLocale.currentLocale.languageCode
            if (myLang.contains("es", ignoreCase = true)) {
                "ES"
            } else {
                "EN"
            }
        } catch (e: Exception) {
            "EN"
        }
    }

    actual fun getDecimalSeparator(): Char {
        val formatter = NSNumberFormatter()
        formatter.locale = NSLocale.currentLocale
        return formatter.decimalSeparator.firstOrNull() ?: '.'
    }

    actual fun getGroupingSeparator(): Char {
        val formatter = NSNumberFormatter()
        formatter.locale = NSLocale.currentLocale
        return formatter.groupingSeparator.firstOrNull() ?: '.'
    }

    actual fun getZeroDigit(): Char {
        val formatter = NSNumberFormatter()
        formatter.locale = NSLocale.currentLocale
        return formatter.zeroSymbol?.firstOrNull() ?: '0'
    }
}