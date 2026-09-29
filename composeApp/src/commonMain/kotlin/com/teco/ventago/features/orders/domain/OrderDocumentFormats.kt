package com.teco.ventago.features.orders.domain

internal fun buildOrderDocumentFormats(saveAsDraft: Boolean): List<String> = buildList {
    add("PDF")
    add("XML")
    if (!saveAsDraft) {
        add("TICKET")
    }
}
