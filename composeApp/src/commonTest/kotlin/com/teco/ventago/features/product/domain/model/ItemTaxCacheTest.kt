package com.teco.ventago.features.product.domain.model

import com.teco.ventago.features.product.domain.preferLoadedMenu
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ItemTaxCacheTest {

    @Test
    fun readsTaxFromLegacyCacheKeyAndFromApiKey() {
        assertEquals(7, menu("\"taxPercent\": 7").findItem(9)?.taxPercent)
        assertEquals(7, menu("\"tax_percent\": \"7\"").findItem(9)?.taxPercent)
        assertEquals(7, menu("\"tax_percent\": 7.0").findItem(9)?.taxPercent)
        assertEquals(0, menu("\"tax_percent\": 0, \"taxPercent\": 7").findItem(9)?.taxPercent)
        assertNull(menu("").findItem(9)?.taxPercent)
    }

    @Test
    fun roundTripKeepsExemptAndDropsUnknown() {
        val taxed = menu("\"taxPercent\": 7")
        val saved = taxed.asJSONObject!!
        val item = saved.itemJson()
        assertEquals("7", item["tax_percent"]!!.jsonPrimitive.content)
        assertEquals("7", item["taxPercent"]!!.jsonPrimitive.content)
        assertEquals(7, Products(saved).findItem(9)?.taxPercent)

        val exempt = menu("\"tax_percent\": 0")
        assertEquals("0", exempt.asJSONObject!!.itemJson()["tax_percent"]!!.jsonPrimitive.content)

        val unknownItem = menu("").asJSONObject!!.itemJson()
        assertNull(unknownItem["tax_percent"])
        assertNull(unknownItem["taxPercent"])
    }

    @Test
    fun remoteMenuWinsOverLateCache() {
        val remote = menu("\"tax_percent\": 7")
        val cached = menu("\"taxPercent\": 0")
        assertEquals(7, preferLoadedMenu(remote, cached).findItem(9)?.taxPercent)
        assertEquals(0, preferLoadedMenu(null, cached).findItem(9)?.taxPercent)
    }

    private fun menu(taxFields: String): Products {
        val tax = taxFields.takeIf { it.isNotBlank() }?.let { ", $it" }.orEmpty()
        val json = """
            {"idMenu":1,"active":1,"categories":[{"idCategory":1,"category":"Bebidas","desc":"","active":1,"order":1,"items":[{"idItem":9,"name":"Agua","descripcion":"","img":"","price":1.5,"active":1,"order":1,"product_type":1$tax}]}]}
        """.trimIndent()
        return Products(Json.parseToJsonElement(json).jsonObject)
    }
}

private fun JsonObject.itemJson(): JsonObject {
    return this["categories"]!!.jsonArray.first().jsonObject["items"]!!.jsonArray.first().jsonObject
}
