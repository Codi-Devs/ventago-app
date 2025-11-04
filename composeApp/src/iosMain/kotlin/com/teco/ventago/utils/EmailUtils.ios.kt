package com.teco.ventago.utils

import platform.Foundation.NSURL
import platform.UIKit.UIApplication


fun openEmailIntent(email: String) {
    val mailtoUrl = "mailto:$email"
    val nsUrl = NSURL.URLWithString(mailtoUrl)

    if (nsUrl != null && UIApplication.sharedApplication.canOpenURL(nsUrl)) {
        UIApplication.sharedApplication.openURL(nsUrl)
    }
}