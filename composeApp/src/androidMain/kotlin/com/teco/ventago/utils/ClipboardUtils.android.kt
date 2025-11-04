package com.teco.ventago.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import org.koin.java.KoinJavaComponent

actual fun copyToClipboard(label: String, text: String) {
    val context: Context = KoinJavaComponent.getKoin().get()
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
}