package com.teco.ventago.features.quotes.ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero
import platform.Foundation.*
import platform.WebKit.WKWebView

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PdfPreview(url: String, modifier: Modifier) {
    UIKitView(
        modifier = modifier,
        factory = {
            val webView = WKWebView(frame = CGRectZero.readValue())
            webView.allowsLinkPreview = false
            webView.scrollView.bounces = false
            webView.scrollView.scrollEnabled = false
            webView.userInteractionEnabled = false
            if (url.startsWith("data:application/pdf", ignoreCase = true)) {
                val safeDataUri = (url as NSString)
                    .stringByReplacingOccurrencesOfString("\"", withString = "&quot;")
                val html = """
                    <html>
                    <body style="margin:0;padding:0;">
                        <iframe src="$safeDataUri" style="width:100%;height:100%;border:none;"></iframe>
                    </body>
                    </html>
                """.trimIndent()
                webView.loadHTMLString(html, baseURL = null)
            } else {
                val encodedUrl = (url as NSString)
                    .stringByAddingPercentEncodingWithAllowedCharacters(NSCharacterSet.URLQueryAllowedCharacterSet())
                    ?: url
                val viewerUrl = "https://docs.google.com/gview?embedded=true&url=$encodedUrl"
                val nsUrl = NSURL.URLWithString(viewerUrl)
                if (nsUrl != null) {
                    webView.loadRequest(NSURLRequest.requestWithURL(nsUrl))
                }
            }
            webView
        },
        update = { view ->
            if (url.startsWith("data:application/pdf", ignoreCase = true)) {
                val safeDataUri = (url as NSString)
                    .stringByReplacingOccurrencesOfString("\"", withString = "&quot;")
                val html = """
                    <html>
                    <body style="margin:0;padding:0;">
                        <iframe src="$safeDataUri" style="width:100%;height:100%;border:none;"></iframe>
                    </body>
                    </html>
                """.trimIndent()
                view.loadHTMLString(html, baseURL = null)
            } else {
                val encodedUrl = (url as NSString)
                    .stringByAddingPercentEncodingWithAllowedCharacters(NSCharacterSet.URLQueryAllowedCharacterSet())
                    ?: url
                val viewerUrl = "https://docs.google.com/gview?embedded=true&url=$encodedUrl"
                val currentUrl = view.URL?.absoluteString ?: ""
                if (currentUrl != viewerUrl) {
                    val nsUrl = NSURL.URLWithString(viewerUrl)
                    if (nsUrl != null) {
                        view.loadRequest(NSURLRequest.requestWithURL(nsUrl))
                    }
                }
            }
        },
        properties = UIKitInteropProperties(isInteractive = true, isNativeAccessibilityEnabled = true)
    )
}
