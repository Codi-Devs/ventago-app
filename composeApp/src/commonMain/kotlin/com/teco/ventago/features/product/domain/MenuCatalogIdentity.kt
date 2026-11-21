package com.teco.ventago.features.product.domain

import com.teco.ventago.features.product.domain.model.Category
import com.teco.ventago.features.product.domain.model.Item
import com.teco.ventago.features.product.domain.model.Products

/**
 * A repeated category id or item id makes Compose abort the process: list and
 * grid keys must be unique. The same item id in two categories is the same
 * product, so the first copy wins.
 */
internal fun Products.hasDuplicateCatalogIds(): Boolean {
    val categoryIds = HashSet<Int>()
    val itemIds = HashSet<Int>()
    for (category in categories) {
        if (!categoryIds.add(category.id)) return true
        for (item in category.items) {
            if (!itemIds.add(item.itemId)) return true
        }
    }
    return false
}

internal fun Products.withoutDuplicateCatalogIds(): Products {
    if (!hasDuplicateCatalogIds()) return this
    val categoryIds = HashSet<Int>()
    val itemIds = HashSet<Int>()
    val uniqueCategories = ArrayList<Category>(categories.size)
    for (category in categories) {
        if (!categoryIds.add(category.id)) continue
        val uniqueItems = ArrayList<Item>(category.items.size)
        for (item in category.items) {
            if (itemIds.add(item.itemId)) uniqueItems.add(item)
        }
        uniqueCategories.add(
            if (uniqueItems.size == category.items.size) category
            else category.copy(items = uniqueItems)
        )
    }
    return copy(categories = uniqueCategories)
}
