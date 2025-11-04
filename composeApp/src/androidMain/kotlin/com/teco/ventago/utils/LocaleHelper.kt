package com.teco.ventago.utils

import java.text.DecimalFormat
import java.util.Locale

actual object LocaleHelper {

    actual fun getLocale(): String {
        return try {
            if (Locale.getDefault().language.contains("es", ignoreCase = true)) {
                "ES"
            } else {
                "EN"
            }
        } catch (e: Exception) {
            "EN"
        }
    }

    actual fun getDecimalSeparator(): Char {
        return DecimalFormat().decimalFormatSymbols.decimalSeparator
    }

    actual fun getGroupingSeparator(): Char {
        return DecimalFormat().decimalFormatSymbols.groupingSeparator
    }

    actual fun getZeroDigit(): Char {
        return DecimalFormat().decimalFormatSymbols.zeroDigit
    }
}