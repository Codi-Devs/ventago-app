package com.teco.ventago.utils

import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.Foundation.*
import platform.UIKit.*

actual fun openDialer(phoneNumber: String) {
    val urlString = "tel:$phoneNumber"
    val nsUrl = NSURL.URLWithString(urlString)
    if (nsUrl != null && UIApplication.sharedApplication.canOpenURL(nsUrl)) {
        UIApplication.sharedApplication.openURL(nsUrl)
    }
}

actual fun openWhatsappMessage(phoneNumber: String?, message: String) {
    val baseUrl = "https://wa.me/"
    val encodedText = message.encodeURL()
    val urlString = phoneNumber?.let {
        "$baseUrl${it.replace("+", "").replace(" ", "")}/?text=$encodedText"
    } ?: "$baseUrl?text=$encodedText"

    openUrl(urlString)
}

actual fun openSms(phoneNumber: String, message: String) {
    val encodedText = message.encodeURL()
    val smsUrl = "sms:$phoneNumber&body=$encodedText"
    openUrl(smsUrl)
}

// Helper function
private fun openUrl(url: String) {
    val nsUrl = NSURL.URLWithString(url)
    if (UIApplication.sharedApplication.canOpenURL(nsUrl!!)) {
        UIApplication.sharedApplication.openURL(nsUrl!!)
    }
}

// Basic URL encoding
private fun String.encodeURL(): String {
    val allowed = NSCharacterSet.URLQueryAllowedCharacterSet()
    return (this as NSString).stringByAddingPercentEncodingWithAllowedCharacters(allowed) ?: this
}