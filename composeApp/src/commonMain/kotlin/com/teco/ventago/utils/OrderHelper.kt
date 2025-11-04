package com.teco.ventago.utils

fun formatShippingMethod(value: String, suffix: String = "") : String {
    if (value.isBlank() || value.isEmpty()) {
        return ""
    }

    if (value == "0" || value == "0.0" || value == "0.00") {
        return "Gratis"
    }

    return "$value $suffix"
}
