package com.teco.ventago.features.orders.domain.models//package com.teco.ventago.features.orders.domain.models
//
//import kotlinx.serialization.Serializable
//import kotlinx.serialization.json.JsonElement
//import kotlinx.serialization.json.double
//import kotlinx.serialization.json.int
//import kotlinx.serialization.json.jsonPrimitive
//
//@Serializable
//class ShippingAddress(
//    var reference: String,
//    var address: String,
//    var latitude: Double,
//    var longitude: Double,
//    var placeId: String,
//    var id: Int
//) {
//
//    companion object {
//        fun fromMap(map: Map<String, JsonElement>): ShippingAddress {
//            return ShippingAddress(
//                reference = map["reference"]!!.jsonPrimitive.content,
//                address = map["address"]!!.jsonPrimitive.content,
//                latitude = map["latitude"]!!.jsonPrimitive.double,
//                longitude = map["longitude"]!!.jsonPrimitive.double,
//                placeId = map["place_id"]!!.jsonPrimitive.content,
//                id = map["id"]!!.jsonPrimitive.int
//            )
//        }
//    }
//
//
//
//}