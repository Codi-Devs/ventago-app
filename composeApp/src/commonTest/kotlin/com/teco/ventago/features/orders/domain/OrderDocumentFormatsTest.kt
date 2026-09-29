package com.teco.ventago.features.orders.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class OrderDocumentFormatsTest {
    @Test
    fun confirmedOrdersAlwaysRequestTicket() {
        assertEquals(
            listOf("PDF", "XML", "TICKET"),
            buildOrderDocumentFormats(saveAsDraft = false),
        )
    }

    @Test
    fun draftOrdersDoNotRequestTicket() {
        assertEquals(
            listOf("PDF", "XML"),
            buildOrderDocumentFormats(saveAsDraft = true),
        )
    }
}
