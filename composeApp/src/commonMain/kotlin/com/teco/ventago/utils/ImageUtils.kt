package com.teco.ventago.utils

import coil3.PlatformContext
import coil3.request.ImageRequest


interface ImageSaver {
    fun saveImage(image: Any, fileName: String): String?
}

object ImageSaverFactory {
    fun create(): ImageSaver = createImageSaver()
}

expect fun createImageSaver(): ImageSaver

expect fun openFileInGallery(filePath: String)

expect fun shareInvoice(path: String)

internal expect fun getImageRequest(context: PlatformContext, url: String): ImageRequest

