package com.teco.ventago.features.quotes.ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import androidx.compose.ui.interop.UIKitInteropProperties
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSCharacterSet
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.Foundation.NSString
import platform.WebKit.WKWebView

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
            val encodedUrl = (url as NSString)
                .stringByAddingPercentEncodingWithAllowedCharacters(NSCharacterSet.URLQueryAllowedCharacterSet())
                ?: url
            val viewerUrl = "https://docs.google.com/gview?embedded=true&url=$encodedUrl"
            val nsUrl = NSURL.URLWithString(viewerUrl)
            if (nsUrl != null) {
                webView.loadRequest(NSURLRequest.requestWithURL(nsUrl))
            }
            webView
        },
        update = { view ->
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
        },
        properties = UIKitInteropProperties(isInteractive = true, isNativeAccessibilityEnabled = true)
    )
}
