@file:OptIn(ExperimentalForeignApi::class)

package com.teco.ventago.core

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.create
import platform.Foundation.writeToURL
import platform.UIKit.*
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@OptIn(BetaInteropApi::class)
private fun ByteArray.toNSData(): NSData =
    usePinned { pinned -> NSData.create(bytes = pinned.addressOf(0), length = size.toULong()) }

/** Try to find the top-most presented UIViewController to present the share sheet. */
private fun topViewController(): UIViewController? {
    val app = UIApplication.sharedApplication
    // iOS 13+ – walk connected scenes first
    val keyWindow = UIApplication.sharedApplication.keyWindow
    var top = keyWindow?.rootViewController
    while (top?.presentedViewController != null) {
        top = top.presentedViewController
    }
    return top
}

class IosPdfSharer : PdfSharer {

    override fun sharePdf(filename: String, bytes: ByteArray) {
        // Write PDF to temporary directory
        val tmpPath = NSTemporaryDirectory() + filename
        val fileUrl = NSURL.fileURLWithPath(tmpPath)
        val data = bytes.toNSData()
        data.writeToURL(fileUrl, true)

        val activityViewController = UIActivityViewController(activityItems = listOf(fileUrl), applicationActivities = null)

        val keyWindow = UIApplication.sharedApplication.keyWindow
        val rootViewController = keyWindow?.rootViewController

        rootViewController?.let { viewController ->
            activityViewController.popoverPresentationController?.sourceView = viewController.view
            viewController.presentViewController(activityViewController, animated = true) {
                println("Invoice shared successfully")
            }
        } ?: run {
            println("Unable to find root view controller for sharing")
        }

        // Present native share sheet on the main thread
//        dispatch_async(dispatch_get_main_queue()) {
//            val activityVC = UIActivityViewController(
//                activityItems = listOf(fileUrl),
//                applicationActivities = null
//            )
//            // iPad popover anchor (avoid crash on iPad)
//            activityVC.setModalPresentationStyle(UIModalPresentationPopover)
//            val top = topViewController()
//            val pop = activityVC.popoverPresentationController
//            if (pop != null && top?.view != null) {
//                pop.sourceView = top.view
//                pop.sourceRect = top.view.bounds
//                pop.permittedArrowDirections = 0u
//            }
//            top?.presentViewController(activityVC, true, completion = null)
//        }
    }

    override fun openPdf(filename: String, bytes: ByteArray) {
        sharePdf(filename, bytes)
    }
}