package com.teco.ventago.utils

import android.content.Context
import android.content.Intent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import org.koin.java.KoinJavaComponent

actual fun openCustomTab(url: String){
    val context: Context = KoinJavaComponent.getKoin().get()
    val builder = CustomTabsIntent.Builder()
    val customTabsIntent = builder.build()
    customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    customTabsIntent.launchUrl(context, url.toUri())
}

actual fun shareLink(url: String) {
    try {
        val context: Context = KoinJavaComponent.getKoin().get()
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.setType("text/plain")
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Pagos")
        shareIntent.putExtra(Intent.EXTRA_TEXT, url)
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val chooserIntent =
            Intent.createChooser(shareIntent, "Compartir")
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooserIntent)
    } catch (_: Exception) { }
}