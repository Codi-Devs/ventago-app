package com.teco.ventago.features.product.domain.model


import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

data class Category(
    val id: Int,
    val name: String,
    val desc: String,
    val active: Boolean,
    val items: List<Item>,
    var order: Int,
) {
    constructor(response: JsonObject): this(
        id = response["idCategory"]?.jsonPrimitive?.int ?: -1,
        name = response["category"]?.jsonPrimitive?.content ?: "",
        desc = response["desc"]?.jsonPrimitive?.content ?: "",
        active = (response["active"]?.jsonPrimitive?.int ?: 0) == 1,
        items = Item.listFromMap(response["items"]?.jsonArray ?: JsonArray(emptyList())),
        order = response["order"]?.jsonPrimitive?.int ?: -1,
    )

    val asJSONObject: JsonObject?
        get() {
            var category: JsonObject?
            val data = mutableMapOf<String, JsonElement>()
            try {
                data["idCategory"] = JsonPrimitive(id)
                data["category"] = JsonPrimitive(name)
                data["desc"] = JsonPrimitive(desc)
                data["active"] = JsonPrimitive(if (active) 1 else 0)
                data["order"] = JsonPrimitive(order)
                val arr = JsonArray(items.map { it.asJSONObject!! })
                data["items"] = arr
                category = JsonObject(data)
            } catch (e: Exception) {
                e.printStackTrace()
                category = null
            }
            return category
        }


    fun getLastItemOrder(): Int {
        var aux = 0
        for (i in items.indices) {
            if (items[i].order > aux) {
                aux = items[i].order
            }
        }
        return aux
    }

    fun findItemInCategory(itemId: Int): Item? {
        for (i in items.indices) {
            if (items[i].itemId == itemId) {
                return items[i]
            }
        }
        return null
    }

    fun countItemsWithImg(): Int {
        var count = 0
        for (i in items.indices) {
            if (!items[i].img.equals("null", ignoreCase = true)) {
                count++
            }
        }
        return count
    }

    companion object {
        fun listFromMap(arr: JsonArray): List<Category> {
            val list = mutableListOf<Category>()
            for (item in arr) {
                val map = Json.decodeFromJsonElement<Map<String, JsonElement>>(item)
                val element = fromMap(map)
                list.add(element)
            }
            return list
        }

        private fun fromMap(map: Map<String, JsonElement>): Category {
            return Category(
                id = map["idCategory"]?.jsonPrimitive?.int ?: -1,
                name = map["category"]?.jsonPrimitive?.content ?: "",
                desc = map["desc"]?.jsonPrimitive?.content ?: "",
                active = (map["active"]?.jsonPrimitive?.int ?: 0) == 1,
                items = Item.listFromMap(map["items"]?.jsonArray ?: JsonArray(emptyList())),
                order = map["order"]?.jsonPrimitive?.int ?: -1,
            )
        }

        fun newCategoryFromJson(json: JsonObject): Category {
            return Category(
                id = json["id_category"]?.jsonPrimitive?.int ?: -1,
                name = json["name"]?.jsonPrimitive?.content ?: "",
                desc = json["desc"]?.jsonPrimitive?.content ?: "",
                active = (json["active"]?.jsonPrimitive?.int ?: 0) == 1,
                items = emptyList(),
                order = json["order"]?.jsonPrimitive?.int ?: -1,
            )
        }
    }
}