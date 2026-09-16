package com.teco.ventago.utils

actual object LocaleHelper {

    actual fun getLocale(): String = "ES"

    actual fun getDecimalSeparator(): Char = '.'

    actual fun getGroupingSeparator(): Char = ','

    actual fun getZeroDigit(): Char = '0'
}
