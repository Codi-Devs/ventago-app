package com.teco.ventago.utils

import kotlin.math.abs
import kotlin.math.round

private const val DISPLAY_CURRENCY_SYMBOL = "$"

fun formatNumberToMoney(amount: String): String {
    val parsed = amount.trim().toDoubleOrNull() ?: 0.0
    return formatNumberToMoney(parsed)
}

fun formatNumberToMoney(amount: Double): String {
    val cents = round(abs(amount) * 100.0).toLong()
    val units = cents / 100
    val decimals = (cents % 100).toString().padStart(2, '0')
    val grouped = groupThousands(units)
    val formatted = "$DISPLAY_CURRENCY_SYMBOL$grouped.$decimals"
    return if (amount < 0.0 && cents != 0L) "-$formatted" else formatted
}

private fun groupThousands(value: Long): String {
    return value.toString()
        .reversed()
        .chunked(3)
        .joinToString(",")
        .reversed()
}
