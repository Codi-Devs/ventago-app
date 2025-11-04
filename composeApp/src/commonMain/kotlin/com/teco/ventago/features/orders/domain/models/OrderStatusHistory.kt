package com.teco.ventago.features.orders.domain.models//package com.teco.ventago.features.orders.domain.models
//
//import kotlinx.serialization.Serializable
//import kotlinx.serialization.json.Json
//import kotlinx.serialization.json.JsonArray
//import kotlinx.serialization.json.JsonElement
//import kotlinx.serialization.json.decodeFromJsonElement
//import kotlinx.serialization.json.int
//import kotlinx.serialization.json.jsonPrimitive
//
//@Serializable
//class OrderStatusHistory(val statusId: Int, val date: String) {
//    fun isCancelled(): Boolean {
//        return statusId == OrderStatus.CANCELLED || statusId == OrderStatus.REJECT
//    }
//
//    fun isCompleted(): Boolean {
//        return statusId == OrderStatus.COMPLETED || statusId == OrderStatus.PAYMENT_FAILED
//    }
//
//    companion object {
//
//        fun listFromMap(arr: JsonArray): List<OrderStatusHistory> {
//            val list = mutableListOf<OrderStatusHistory>()
//            for (item in arr) {
//                val map = Json.decodeFromJsonElement<Map<String, JsonElement>>(item)
//                val orderStatusHistory = fromMap(map)
//                list.add(orderStatusHistory)
//            }
//            return list
//        }
//
//        fun fromMap(map: Map<String, JsonElement>): OrderStatusHistory {
//            return OrderStatusHistory(
//                statusId = map["order_status_id"]!!.jsonPrimitive.int,
//                date = map["created_at"]!!.jsonPrimitive.content
//            )
//        }
//    }
//
//}