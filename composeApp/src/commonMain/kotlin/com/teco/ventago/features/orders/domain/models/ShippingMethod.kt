package com.teco.ventago.features.orders.domain.models//package com.teco.ventago.features.orders.domain.models
//
//import com.teco.ventago.utils.RequestClass
//import kotlinx.serialization.json.Json
//import kotlinx.serialization.json.JsonArray
//import kotlinx.serialization.json.JsonElement
//import kotlinx.serialization.json.decodeFromJsonElement
//import kotlinx.serialization.json.double
//import kotlinx.serialization.json.int
//import kotlinx.serialization.json.jsonPrimitive
//
//data class ShippingMethod(val id: Int, val name: String, val unitPrice: Double): RequestClass() {
//
//    override fun toJson() =
//        """
//            {
//                "method_id": $id,
//                "unit_price": $unitPrice
//            }
//        """.trimIndent()
//
//    companion object {
//
//        const val PICKUP = 1
//        const val DELIVERY = 2
//        const val DELIVERY_FIXED = 3
//        fun fromMap(map: Map<String, JsonElement>): ShippingMethod {
//            return ShippingMethod(
//                id = map["id"]!!.jsonPrimitive.int,
//                name = map["name"]?.jsonPrimitive?.content ?: "",
//                unitPrice = map["unit_price"]!!.jsonPrimitive.double
//            )
//        }
//
//        private fun fromMapConfig(map: Map<String, JsonElement>): ShippingMethod {
//            return ShippingMethod(
//                id = map["method_id"]!!.jsonPrimitive.int,
//                name = map["name"]?.jsonPrimitive?.content ?: "",
//                unitPrice = map["unit_price"]!!.jsonPrimitive.double
//            )
//        }
//
//        fun listFromMap(arr: JsonArray): List<ShippingMethod> {
//            val list = mutableListOf<ShippingMethod>()
//            for (item in arr) {
//                val map = Json.decodeFromJsonElement<Map<String, JsonElement>>(item)
//                val shippingMethod = fromMapConfig(map)
//                list.add(shippingMethod)
//            }
//            return list
//        }
//    }
//
//}