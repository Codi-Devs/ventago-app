package com.teco.ventago.features.product

import com.teco.ventago.features.product.domain.hasDuplicateCatalogIds
import com.teco.ventago.features.product.domain.model.Category
import com.teco.ventago.features.product.domain.model.Item
import com.teco.ventago.features.product.domain.model.ProductType
import com.teco.ventago.features.product.domain.model.Products
import com.teco.ventago.features.product.domain.withoutDuplicateCatalogIds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MenuCatalogIdentityTest {

    @Test
    fun repeatedItemIdIsDetectedAndCollapsedToTheFirstCopy() {
        val limonada = item(6545, "Limonada")
        val menu = menu(category(id = 8, name = "Bebidas", items = listOf(limonada, limonada.copy())))

        assertTrue(menu.hasDuplicateCatalogIds())
        val clean = menu.withoutDuplicateCatalogIds()

        assertFalse(clean.hasDuplicateCatalogIds())
        assertEquals(listOf(6545), clean.categories.single().items.map { it.itemId })
    }

    @Test
    fun sameItemInTwoCategoriesKeepsTheFirstCategory() {
        val limonada = item(6545, "Limonada")
        val menu = menu(
            category(id = 8, name = "Bebidas", items = listOf(limonada)),
            category(id = 9, name = "Otros", items = listOf(limonada.copy(name = "Limonada 2"))),
        )

        val clean = menu.withoutDuplicateCatalogIds()

        assertEquals(listOf(8, 9), clean.categories.map { it.id })
        assertEquals(listOf(6545), clean.categories[0].items.map { it.itemId })
        assertEquals(emptyList(), clean.categories[1].items.map { it.itemId })
    }

    @Test
    fun repeatedCategoryIdKeepsTheFirstCategory() {
        val menu = menu(
            category(id = 8, name = "Bebidas", items = listOf(item(1, "Limonada"))),
            category(id = 8, name = "Bebidas copia", items = listOf(item(2, "Tea"))),
        )

        val clean = menu.withoutDuplicateCatalogIds()

        assertEquals(listOf("Bebidas"), clean.categories.map { it.name })
        assertFalse(clean.hasDuplicateCatalogIds())
    }

    @Test
    fun cleanMenuIsReturnedWithoutCopying() {
        val menu = menu(category(id = 8, name = "Bebidas", items = listOf(item(6545, "Limonada"))))

        assertFalse(menu.hasDuplicateCatalogIds())
        assertSame(menu, menu.withoutDuplicateCatalogIds())
    }

    private fun menu(vararg categories: Category): Products {
        return Products(id = 143, active = true, categories = categories.toList())
    }

    private fun category(
        id: Int,
        name: String,
        items: List<Item>,
    ): Category {
        return Category(
            id = id,
            name = name,
            desc = "",
            active = true,
            items = items,
            order = 1,
        )
    }

    private fun item(id: Int, name: String): Item {
        return Item(
            itemId = id,
            barcode = null,
            sku = null,
            name = name,
            description = "",
            img = "",
            price = 1.0,
            cost = null,
            active = true,
            order = 1,
            taxPercent = 7,
            productType = ProductType.GOOD,
        )
    }
}
