package com.teco.ventago.features.product.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CatalogDeleteStockCopyTest {
    @Test
    fun namesTheProductAndItsStock() {
        assertEquals(
            "No puedes borrar «Coca Cola» porque tiene 12 en stock. Registra una salida de inventario hasta dejarlo en 0 y después podrás borrarlo.",
            CatalogDeleteStockCopy.product("Coca Cola", "12"),
        )
    }

    @Test
    fun namesTheCategoryProductWhenOnlyOneHasStock() {
        val message = CatalogDeleteStockCopy.category(
            "Bebidas",
            listOf("Coca Cola" to "12"),
        )
        assertEquals(
            "No puedes borrar la categoría «Bebidas» porque «Coca Cola» tiene 12 en stock. Registra una salida de inventario hasta dejarlo en 0 y después podrás borrar la categoría.",
            message,
        )
    }

    @Test
    fun listsSeveralProductsAndStopsAtThree() {
        val message = CatalogDeleteStockCopy.category(
            "Bebidas",
            listOf("Coca Cola" to "12", "Jugo" to "3.5", "Agua" to "1", "Té" to "4"),
        )
        assertTrue(message!!.contains("«Coca Cola» (12), «Jugo» (3.5), «Agua» (1) y 1 más"))
    }

    @Test
    fun emptyCategoryCanBeDeleted() {
        assertNull(CatalogDeleteStockCopy.category("Bebidas", emptyList()))
    }
}
