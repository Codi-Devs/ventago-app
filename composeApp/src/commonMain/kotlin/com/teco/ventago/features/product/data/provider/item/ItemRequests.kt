package com.teco.ventago.features.product.data.provider.item

import com.teco.ventago.features.product.domain.model.Item
import com.teco.ventago.features.product.domain.model.OTITax
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray

object ItemRequests {

    fun addItem(item: Item, categoryId: Int): String {
        val otiTaxesJson = item.otiTaxes?.let { buildOtiTaxesJson(it) } ?: "null"
        return """
            {
                "name": "${item.name}",
                "desc": "${item.description}",
                "barcode": "${item.barcode ?: ""}",
                "sku": "${item.sku ?: ""}",
                "price": ${item.price},
                "cost": ${item.cost ?: 0},
                "tax_percent": ${item.taxPercent ?: 0},
                "product_type": ${item.productType.typeId},
                "order": ${item.order},
                "id_category": $categoryId,
                "img": "${item.img}",
                "unit_measure_code": "${item.unitMeasureCode}",
                "isc_rate": ${item.iscRate ?: 0.0},
                "oti_taxes": $otiTaxesJson,
                "is_pharma": ${item.isPharma},
                "additional_info": ${item.additionalInfo ?: JsonObject(emptyMap())}
            }
        """.trimIndent()
    }


    fun editItem(item: Item): String {
        val otiTaxesJson = item.otiTaxes?.let { buildOtiTaxesJson(it) } ?: "null"
        return """
            {
                "id": ${item.itemId},
                "name": "${item.name}",
                "desc": "${item.description}",
                "barcode": "${item.barcode ?: ""}",
                "sku": "${item.sku ?: ""}",
                "cost": ${item.cost ?: 0},
                "tax_percent": ${item.taxPercent ?: 0},
                "product_type": ${item.productType.typeId},
                "price": ${item.price},
                "active": ${item.active},
                "img": "${item.img}",
                "unit_measure_code": "${item.unitMeasureCode}",
                "isc_rate": ${item.iscRate ?: 0.0},
                "oti_taxes": $otiTaxesJson,
                "is_pharma": ${item.isPharma},
                "additional_info": ${item.additionalInfo ?: JsonObject(emptyMap())}
            }
        """.trimIndent()
    }


    fun removeItem(id: Int): String =
        """
            {
                "id": $id
            }
        """.trimIndent()

    fun changeItemOrder(items: List<Item>, value: Double): String {
        val arr = buildJsonArray {
            for (item in items) {
                val map = mutableMapOf<String, JsonElement>()
                map["id"] = JsonPrimitive(item.itemId)
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

    // ✅ Helper for converting list of OTITax to JSON
    private fun buildOtiTaxesJson(otiTaxes: List<OTITax>): String {
        val arr = buildJsonArray {
            otiTaxes.forEach { oti ->
                add(
                    JsonObject(
                        mapOf(
                            "id" to JsonPrimitive(oti.id),
                            "rate" to JsonPrimitive(oti.rate)
                        )
                    )
                )
            }
        }
        return arr.toString()
    }

}