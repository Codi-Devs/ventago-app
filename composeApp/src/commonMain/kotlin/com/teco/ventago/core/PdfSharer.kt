package com.teco.ventago.core

interface PdfSharer {
    fun sharePdf(filename: String, bytes: ByteArray)
    fun openPdf(filename: String, bytes: ByteArray)
}