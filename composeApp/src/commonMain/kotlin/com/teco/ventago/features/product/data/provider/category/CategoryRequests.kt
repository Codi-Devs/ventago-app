package com.teco.ventago.features.product.data.provider.category

import com.teco.ventago.features.product.domain.model.Category
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray

object CategoryRequests {

    fun setActive(categoryId: Int, active: Boolean): String =
        """
            {
                "id": $categoryId,
                "active": $active
            }
        """.trimIndent()

    fun addCategory(item: Category, menuId: Int): String =
        """
            {
                "name": "${item.name}",
                "desc": "${item.desc}",
                "id_menu": $menuId,
                "order": ${item.order}
            }
        """.trimIndent()

    fun editCategory(category: Category): String =
        """
            {
                "name": "${category.name}",
                "desc": "${category.desc}",
                "id": ${category.id},
                "active": ${category.active}
            }
        """.trimIndent()

    fun removeCategory(id: Int): String =
        """
            {
                "id_cat": $id
            }
        """.trimIndent()


    fun changeCategoryOrder(categories: List<Category>, value: Double): String {
        val arr = buildJsonArray {
            for (item in categories) {
                val map = mutableMapOf<String, JsonElement>()
                map["idCategory"] = JsonPrimitive(item.id)
                map["order"] = JsonPrimitive(item.order)
                val obj = JsonObject(map)
                add(obj)
            }
        }

        return """
            {
                "cats": $arr,
                "value": "$value"
            }
        """.trimIndent()
    }
}