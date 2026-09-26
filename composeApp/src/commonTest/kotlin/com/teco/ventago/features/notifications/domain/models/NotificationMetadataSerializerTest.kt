package com.teco.ventago.features.notifications.domain.models

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NotificationMetadataSerializerTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun decodesObjectMetadata() {
        val page = json.decodeFromString<NotificationsPage>(
            """{"items":[{"id":1,"metadata":{"order_ids":[2734]}}],"total":1,"limit":20,"offset":0}"""
        )
        val ids = page.items.first().metadata.getValue("order_ids").toString()
        assertEquals("[2734]", ids)
    }

    @Test
    fun decodesByteArrayMetadata() {
        val raw = """{"order_ids":[2734],"window_start":"2026-09-25"}"""
        val bytes = raw.map { it.code }.joinToString(",")
        val page = json.decodeFromString<NotificationsPage>(
            """{"items":[{"id":409,"metadata":[$bytes]}],"total":1,"limit":20,"offset":0}"""
        )
        assertEquals("2026-09-25", page.items.first().metadata.getValue("window_start").jsonPrimitive.content)
        assertTrue(page.items.first().metadata.containsKey("order_ids"))
    }

    @Test
    fun treatsNullItemsAsEmptyList() {
        val page = json.decodeFromString<NotificationsPage>(
            """{"items":null,"total":0,"limit":20,"offset":0}"""
        )
        assertEquals(emptyList(), page.items)
        assertEquals(0, page.total)
    }
}
