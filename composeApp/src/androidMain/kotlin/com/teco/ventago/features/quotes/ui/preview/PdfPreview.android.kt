package com.teco.ventago.features.quotes.ui.preview

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import java.net.URLEncoder

@Composable
actual fun PdfPreview(url: String, modifier: Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.setSupportZoom(false)
                settings.builtInZoomControls = false
                settings.displayZoomControls = false
                webViewClient = WebViewClient()
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                setOnLongClickListener { true }
                isLongClickable = false
                isClickable = false
                isFocusable = false
                isFocusableInTouchMode = false
                setOnTouchListener { _, _ -> true }
            }
        },
        update = { webView ->
            val encoded = try {
                URLEncoder.encode(url, "UTF-8")
            } catch (_: Exception) {
                url
            }
            val viewerUrl = "https://docs.google.com/gview?embedded=true&url=$encoded"
            if (webView.url != viewerUrl) {
                webView.loadUrl(viewerUrl)
            }
        }
    )
}
