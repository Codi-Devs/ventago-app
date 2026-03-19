package com.teco.ventago.utils

import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private const val QUANTITY_SCALE_FACTOR = 10_000L
private const val QUANTITY_MIN_VALUE = 0.0001
private const val QUANTITY_ROUNDING_HALF = QUANTITY_SCALE_FACTOR / 2

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
    return (this * 10.0.pow(scale)).roundToLong()
}

fun Long.toScaledDouble(scale: Int = 2): Double {
    return this / 10.0.pow(scale)
}

fun Long.toDecimalString(): String {
    // Avoid floating-point precision errors by working with integers
    val integerPart = this / 100
    val fractionalPart = this % 100
    // Ensure fractional part is always 2 digits (e.g., 5 becomes "05")
    val fractionalString = fractionalPart.absoluteValue.toString().padStart(2, '0')
    return "$integerPart.$fractionalString"
}

fun sanitizeQuantityInput(raw: String): String {
    if (raw.isBlank()) return ""
    val normalizedRaw = raw.trim().replace(',', '.')
    val builder = StringBuilder()
    var hasSeparator = false

    normalizedRaw.forEach { ch ->
        when {
            ch.isDigit() -> builder.append(ch)
            ch == '.' && !hasSeparator -> {
                if (builder.isEmpty()) builder.append('0')
                builder.append('.')
                hasSeparator = true
            }
        }
    }

    val sanitized = builder.toString()
    if (sanitized.isEmpty()) return ""

    val parts = sanitized.split('.', limit = 2)
    val integerPart = parts[0]
    val decimalPart = parts.getOrNull(1)?.take(4).orEmpty()

    return if (hasSeparator) {
        "$integerPart.$decimalPart"
    } else {
        integerPart
    }
}

fun normalizeQuantity(value: Double, minValue: Double = QUANTITY_MIN_VALUE): Double {
    if (!value.isFinite()) return minValue
    val scaled = (value * QUANTITY_SCALE_FACTOR).roundToLong()
    val quantized = scaled / QUANTITY_SCALE_FACTOR.toDouble()
    return quantized.coerceAtLeast(minValue)
}

fun String.toNormalizedQuantityOrNull(minValue: Double = QUANTITY_MIN_VALUE): Double? {
    val sanitized = sanitizeQuantityInput(this)
    if (sanitized.isBlank()) return null
    val parsed = sanitized.toDoubleOrNull() ?: return null
    return normalizeQuantity(parsed, minValue)
}

fun Double.toQuantityUiString(): String {
    val safeValue = if (this.isFinite()) this.coerceAtLeast(0.0) else 0.0
    val scaled = (safeValue * QUANTITY_SCALE_FACTOR).roundToLong()
    val integerPart = scaled / QUANTITY_SCALE_FACTOR
    val decimalPart = (scaled % QUANTITY_SCALE_FACTOR).absoluteValue
        .toString()
        .padStart(4, '0')
        .trimEnd('0')
    return if (decimalPart.isEmpty()) integerPart.toString() else "$integerPart.$decimalPart"
}

fun Double.toQuantityRequestString(): String {
    val normalized = normalizeQuantity(this)
    val scaled = (normalized * QUANTITY_SCALE_FACTOR).roundToLong()
    val integerPart = scaled / QUANTITY_SCALE_FACTOR
    val decimalPart = (scaled % QUANTITY_SCALE_FACTOR).absoluteValue.toString().padStart(4, '0')
    return "$integerPart.$decimalPart"
}

fun multiplyCentsByQuantity(cents: Long, quantity: Double): Long {
    if (cents == 0L) return 0L
    val normalized = normalizeQuantity(quantity)
    val quantityScaled = (normalized * QUANTITY_SCALE_FACTOR).roundToLong().coerceAtLeast(1L)
    val numerator = cents * quantityScaled
    val adjusted = if (numerator >= 0L) {
        numerator + QUANTITY_ROUNDING_HALF
    } else {
        numerator - QUANTITY_ROUNDING_HALF
    }
    return adjusted / QUANTITY_SCALE_FACTOR
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

/**
 * Rounds a Long value (in cents) to 2 decimal places.
 * This ensures all monetary calculations maintain 2 decimal precision.
 * Example: 2311 cents (23.11) -> 2311, 2311.5 -> 2312
 */
fun Long.roundTo2Decimals(): Long {
    // Since we're working in cents, we need to handle rounding at the cent level
    // For intermediate calculations that might have fractional cents, round to nearest cent
    return this
}

/**
 * Rounds a Double value to 2 decimal places and converts to Long (cents).
 * Example: 23.115 -> 2312 cents (23.12)
 */
fun Double.roundTo2DecimalsCents(): Long {
    return (this * 100.0).roundToInt().toLong()
}
