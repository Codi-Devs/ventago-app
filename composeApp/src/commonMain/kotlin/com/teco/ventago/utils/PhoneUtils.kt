package com.teco.ventago.utils

fun sanitizePhone(number: String): String {
    return number.replace("\\D".toRegex(), "")
}

expect fun openDialer(phoneNumber: String)

expect fun openWhatsappMessage(phoneNumber: String?, message: String)
expect fun openSms(phoneNumber: String, message: String)