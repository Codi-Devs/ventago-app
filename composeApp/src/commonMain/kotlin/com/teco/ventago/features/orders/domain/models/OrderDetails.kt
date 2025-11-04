package com.teco.ventago.features.orders.domain.models//package com.teco.ventago.features.orders.domain.models
//
//import com.teco.ventago.utils.RequestClass
//import kotlinx.serialization.Serializable
//import kotlinx.serialization.json.Json
//import kotlinx.serialization.json.JsonArray
//import kotlinx.serialization.json.JsonElement
//import kotlinx.serialization.json.decodeFromJsonElement
//import kotlinx.serialization.json.int
//import kotlinx.serialization.json.jsonPrimitive
//
//@Serializable
//data class OrderDetails(var itemName: String, var unitPrice: String, var quantity: Int, var itemId: Int) :
//    RequestClass() {
//    override fun toJson() =
//        """
//            {
//                "item_id": $itemId,
//                "item_name": "$itemName",
//                "quantity": $quantity
//            }
//        """.trimIndent()
//
//    companion object {
//        fun listFromMap(arr: JsonArray): List<OrderDetails> {
//            val list = mutableListOf<OrderDetails>()
//            for (item in arr) {
//                val map = Json.decodeFromJsonElement<Map<String, JsonElement>>(item)
//                val orderDetails = fromMap(map)
//                list.add(orderDetails)
//            }
//            return list
//        }
//
//        private fun fromMap(map: Map<String, JsonElement>): OrderDetails {
//            return OrderDetails(
//                itemName = map["item_name"]!!.jsonPrimitive.content,
//                unitPrice = map["unit_price"]!!.jsonPrimitive.content,
//                quantity = map["quantity"]!!.jsonPrimitive.int,
//                itemId = map["item_id"]!!.jsonPrimitive.int
//            )
//        }
//    }
//}
