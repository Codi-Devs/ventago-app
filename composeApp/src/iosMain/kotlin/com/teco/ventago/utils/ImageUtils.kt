package com.teco.ventago.utils

import coil3.PlatformContext
import coil3.request.ImageRequest
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.writeToURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentInteractionController
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.popoverPresentationController

class IOSImageSaver : ImageSaver {
    override fun saveImage(image: Any, fileName: String): String? {
        val uiImage = image as UIImage // Cast the generic image to UIImage

        // Get the PNG representation of the UIImage
        val imageData = UIImagePNGRepresentation(uiImage) ?: return null

        // Define the file path in the app's documents directory
        val fileManager = NSFileManager.defaultManager
        val documentsDirectory = fileManager.URLsForDirectory(
            directory = NSDocumentDirectory,
            inDomains = NSUserDomainMask
        ).first() as NSURL

        val fileURL = documentsDirectory.URLByAppendingPathComponent(fileName)

        if (fileURL != null) {
            return if (imageData.writeToURL(fileURL, true)) {
                fileURL.path // Return the file path as a String
            } else {
                null // Return null if saving fails
            }
        }
        return null
    }

}

actual fun createImageSaver(): ImageSaver {
    return IOSImageSaver()
}


@OptIn(ExperimentalForeignApi::class)
actual fun openFileInGallery(filePath: String) {
    val url = NSURL.fileURLWithPath(filePath) // Create a file URL from the file path
    val documentController = UIDocumentInteractionController().apply {
        this.URL = url // Set the file URL to be opened
    }

    // Get the current window's root view controller
    val keyWindow = UIApplication.sharedApplication.keyWindow
    val rootViewController = keyWindow?.rootViewController

    rootViewController?.let { viewController ->
        documentController.presentOptionsMenuFromRect(
            rect = CGRectMake(0.0, 0.0, 0.0, 0.0), // Use CGRectZero if you don't want a specific rectangle
            inView = viewController.view,
            animated = true
        )
    } ?: run {
        println("Unable to find root view controller")
    }
}


actual fun shareInvoice(path: String) {
    val fileUrl = NSURL.fileURLWithPath(path)

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
}

internal actual fun getImageRequest(context: PlatformContext, url: String): ImageRequest {
    return ImageRequest.Builder(context)
        .data(url)
        .build()
}