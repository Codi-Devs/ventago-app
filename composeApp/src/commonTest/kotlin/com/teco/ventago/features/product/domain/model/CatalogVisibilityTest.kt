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
                      {"idItem":12,"name":"Con fecha","descripcion":"","img":"","price":1,"cost":0,"active":1,"order":3,"deleted_at":"2026-11-03 12:00:00.000000"},
                      {"idItem":13,"name":"Fecha gana","descripcion":"","img":"","price":1,"cost":0,"active":1,"order":4,"deleted":0,"deleted_at":"2026-11-04 00:00:00"},
                      {"idItem":14,"name":"Null","descripcion":"","img":"","price":1,"cost":0,"active":1,"order":5,"deleted":null,"deleted_at":"2026-11-04 00:00:00"},
                      {"idItem":15,"name":"Objeto","descripcion":"","img":"","price":1,"cost":0,"active":1,"order":6,"deleted":{"x":1}}
                    ]
                  }
                ]
                """.trimIndent()
            ).jsonArray
        )

        assertEquals(listOf(2), categories.map { it.id })
        val items = categories.single().items
        assertEquals(listOf(11, 15), items.map { it.itemId })
        assertEquals(false, items.first { it.itemId == 11 }.active)
        assertEquals(true, items.first { it.itemId == 15 }.active)
    }
}
