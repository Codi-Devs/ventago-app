package com.teco.ventago.utils

import android.content.Context
import android.content.Intent
import org.koin.java.KoinJavaComponent
import androidx.core.net.toUri
import java.net.URLEncoder

actual fun openDialer(phoneNumber: String) {
    val context: Context = KoinJavaComponent.getKoin().get()
    val uri = "tel:$phoneNumber".toUri()
    val intent = Intent(Intent.ACTION_DIAL).apply {
        data = uri
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // if not in an Activity context
    }
    context.startActivity(intent)
}

actual fun openWhatsappMessage(phoneNumber: String?, message: String) {
    val context: Context = KoinJavaComponent.getKoin().get()

    val encodedText = URLEncoder.encode(message, Charsets.UTF_8.name())
    val waUrl = phoneNumber?.let { number ->
        "https://wa.me/${number.replace("+", "").replace(" ", "")}/?text=$encodedText"
    } ?: "https://wa.me/?text=$encodedText"

    val intent = Intent(Intent.ACTION_VIEW, waUrl.toUri()).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
        // fallback attempt
        val fallbackIntent = Intent(Intent.ACTION_VIEW, waUrl.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(fallbackIntent)
    }
}

actual fun openSms(phoneNumber: String, message: String) {
    val context: Context = KoinJavaComponent.getKoin().get()

    val uri = "smsto:$phoneNumber".toUri()
    val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
        putExtra("sms_body", message)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    runCatching {
        context.startActivity(intent)
    }.onFailure {
        it.printStackTrace()
    }
}