package com.teco.ventago.features.product.domain.model

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

data class Products(
    val id: Int,
    val active: Boolean,
    val categories: List<Category>,
) {
    constructor(response: JsonObject): this(
        id = response["idMenu"]?.jsonPrimitive?.int ?: -1,
        active = (response["active"]?.jsonPrimitive?.int ?: 0) == 1,
        categories = Category.listFromMap(response["categories"]?.jsonArray ?: JsonArray(emptyList())),
    )

    val asJSONObject: JsonObject?
        get() {
            var menu: JsonObject?
            val data = mutableMapOf<String, JsonElement>()
            try {
                data["idMenu"] = JsonPrimitive(id)
                data["active"] = JsonPrimitive(if (active) 1 else 0)
                val arr = JsonArray(categories.map { it.asJSONObject!! })
                data["categories"] = arr
                menu = JsonObject(data)
            } catch (e: Exception) {
                e.printStackTrace()
                menu = null
            }
            return menu
        }

    fun getLastCategoryOrder(): Int {
        var aux = 0
        for (i in categories.indices) {
            if (categories[i].order > aux) {
                aux = categories[i].order
            }
        }
        return aux
    }

    fun getItemsQty(): Int {
        var count = 0
        for (aux in categories) {
            count += aux.items.size
        }
        return count
    }

    fun findItem(itemId: Int): Item? {
        for (i in categories.indices) {
            val auxItem: Item? =
                categories[i].findItemInCategory(itemId)
            if (auxItem != null) return auxItem
        }
        return null
    }

    fun countItemsWithImages(): Int {
        var count = 0
        for (i in categories.indices) {
            count += categories[i].countItemsWithImg()
        }
        return count
    }
}