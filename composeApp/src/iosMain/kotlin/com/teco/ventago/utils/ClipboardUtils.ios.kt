package com.teco.ventago.utils

import platform.UIKit.UIPasteboard


actual fun copyToClipboard(label: String, text: String) {
    UIPasteboard.generalPasteboard.string = text
}