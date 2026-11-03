package com.teco.ventago.features.product.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray

class CatalogVisibilityTest {
    @Test
    fun listFromMapDropsDeletedCategoriesAndItemsAndKeepsInactiveOnes() {
        val categories = Category.listFromMap(
            Json.parseToJsonElement(
                """
                [
                  {"idCategory":1,"category":"Eliminada","desc":"","active":0,"order":1,"items":[],"deleted":1},
                  {
                    "idCategory":2,
                    "category":"Visible","desc":"","active":1,"order":2,
                    "items":[
                      {"idItem":10,"name":"Borrado","descripcion":"","img":"","price":1,"cost":0,"active":0,"order":1,"deleted":1},
                      {"idItem":11,"name":"Inactivo","descripcion":"","img":"","price":1,"cost":0,"active":0,"order":2},
                      {"idItem":12,"name":"Con fecha","descripcion":"","img":"","price":1,"cost":0,"active":1,"order":3,"deleted_at":"2026-11-03 12:00:00.000000"}
                    ]
                  }
                ]
                """.trimIndent()
            ).jsonArray
        )

        assertEquals(listOf(2), categories.map { it.id })
        assertEquals(listOf(11), categories.single().items.map { it.itemId })
        assertEquals(false, categories.single().items.single().active)
    }
}
