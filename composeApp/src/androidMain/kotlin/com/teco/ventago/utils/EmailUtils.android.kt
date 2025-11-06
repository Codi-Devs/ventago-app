package com.teco.ventago.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent


fun openEmailIntent(email: String, context: Context) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = android.net.Uri.parse("mailto:$email")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // Fallback to ACTION_SEND if no email app is available
        val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            val chooser = Intent.createChooser(fallbackIntent, "Email")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (_: ActivityNotFoundException) { }
    }
}
