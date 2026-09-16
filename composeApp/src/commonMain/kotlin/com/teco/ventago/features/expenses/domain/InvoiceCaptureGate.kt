package com.teco.ventago.features.expenses.domain

internal expect fun decodeInvoiceRaster(bytes: ByteArray, maxSide: Int = 1600): InvoiceRaster?

internal expect suspend fun scanQrFromImageBytes(bytes: ByteArray): String?

internal expect fun fitInvoiceJpegUnderLimit(bytes: ByteArray, maxBytes: Int): ByteArray

fun isInvoiceRasterMime(contentType: String?, fileName: String?): Boolean {
    val mime = contentType.orEmpty().lowercase()
    val name = fileName.orEmpty().lowercase()
    return mime == "image/jpeg" ||
        mime == "image/jpg" ||
        mime == "image/png" ||
        mime == "image/webp" ||
        name.endsWith(".jpg") ||
        name.endsWith(".jpeg") ||
        name.endsWith(".png") ||
        name.endsWith(".webp")
}

fun isInvoicePdf(contentType: String?, fileName: String?): Boolean {
    val mime = contentType.orEmpty().lowercase()
    val name = fileName.orEmpty().lowercase()
    return mime == "application/pdf" || name.endsWith(".pdf")
}
