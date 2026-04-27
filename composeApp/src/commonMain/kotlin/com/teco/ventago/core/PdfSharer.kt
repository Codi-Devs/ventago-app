package com.teco.ventago.core

interface PdfSharer {
    fun sharePdf(filename: String, bytes: ByteArray)
    fun openPdf(filename: String, bytes: ByteArray)
    fun saveImageToGallery(filename: String, bytes: ByteArray, mimeType: String): Boolean
    fun saveFileToDocuments(filename: String, bytes: ByteArray, mimeType: String): Boolean
}
