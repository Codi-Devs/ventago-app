package com.teco.ventago.utils

import platform.Foundation.NSURL
import platform.SafariServices.SFSafariViewController
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.popoverPresentationController

actual fun openCustomTab(url: String){
    val nsUrl = NSURL.URLWithString(url) ?: return

    val safariVC = SFSafariViewController(nsUrl)

    val rootViewController = getRootViewController()
    rootViewController?.presentViewController(safariVC, animated = true, completion = null)
}


actual fun shareLink(url: String) {
    try {
        val activityItems = listOf(url)
        val activityViewController =
            UIActivityViewController(activityItems = activityItems, applicationActivities = null)

        // Required on iPad
        activityViewController.popoverPresentationController?.sourceView = getRootViewController()?.view

        getRootViewController()?.presentViewController(activityViewController, animated = true, completion = null)
    } catch (_: Exception) { }
}

fun getRootViewController(): UIViewController? {
    val application = UIApplication.sharedApplication
    val firstWindowRoot = (application.windows.firstOrNull() as? UIWindow)?.rootViewController
    val root = application.keyWindow?.rootViewController
        ?: firstWindowRoot
        ?: application.delegate?.window?.rootViewController

    var top = root
    while (top?.presentedViewController != null) {
        top = top.presentedViewController
    }

    return top
}
