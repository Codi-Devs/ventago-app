package com.teco.ventago.utils

expect object LocaleHelper {
    fun getLocale(): String

    fun getDecimalSeparator(): Char
    fun getGroupingSeparator(): Char
    fun getZeroDigit(): Char

}