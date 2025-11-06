package com.teco.ventago.utils

import kotlin.math.pow
import kotlin.math.roundToInt

fun isNumeric(toCheck: String): Boolean {
    return toCheck.toDoubleOrNull() != null
}

fun Double.toScaledInt(scale: Int = 2): Int {
    return (this * 10.0.pow(scale)).toInt()
}

fun String.toScaledDouble(scale: Int = 2): Double {
    return this.toDoubleOrNull()?.let { it / 10.0.pow(scale) } ?: 0.0
}

fun String.toLongCents(): Long {
    return this.toDoubleOrNull()?.let { (it * 100).toLong() } ?: 0L
}

fun Double.toLongCents(scale: Int = 2): Long {
    return (this * 10.0.pow(scale)).toLong()
}

fun Long.toScaledDouble(scale: Int = 2): Double {
    return this / 10.0.pow(scale)
}

fun Long.toDecimalString(): String {
    val decimalValue = this / 100.0
    return decimalValue.toString().takeIf { "." in it }?.let {
        val parts = it.split(".")
        "${parts[0]}.${parts[1].padEnd(2, '0').take(2)}"
    } ?: "$decimalValue.00"
}

/**
 * This function is used to parse a string to a double value.
 * If the string is not a valid double, it will return 0.00
 */
fun String.doubleTryParse(): Double {
    try {
        fun String.fullTrim() = trim().replace("\uFEFF", "")
        return this.fullTrim().toDouble()
    } catch (e: Exception) {
        return 0.00
    }
}

val emailRegex =
    "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$".toRegex()

private val strictEmailRegex = """[a-zA-Z0-9+._%\-]{1,256}@[a-zA-Z0-9][a-zA-Z0-9\-]{0,64}(\.[a-zA-Z0-9][a-zA-Z0-9\-]{0,25})+""".toRegex()

/**
 * Formats a Double to a string with exactly 2 decimal places.
 * Multiplatform-compatible alternative to String.format("%.2f", value)
 */
fun Double.formatTwoDecimals(): String {
    val rounded = (this * 100).roundToInt() / 100.0
    val parts = rounded.toString().split(".")
    val intPart = parts[0]
    val decimalPart = parts.getOrNull(1)?.take(2)?.padEnd(2, '0') ?: "00"
    return "$intPart.$decimalPart"
}