package com.teco.ventago.features.orders.domain.models//package com.teco.ventago.features.orders.domain.models
//
//import kotlinx.serialization.Serializable
//import kotlinx.serialization.json.JsonElement
//import kotlinx.serialization.json.intOrNull
//import kotlinx.serialization.json.jsonPrimitive
//
//@Serializable
//data class Customer(
//    var id: Int? = null,
//    var name: String,
//    var phone: String,
//    var email: String,
//    var country: String,
//    var taxId: String? = null,
//    var tags: List<String>? = null,
//    ) {
//
//    fun toJson() =
//        """
//            {
//                "id": ${id ?: "null"},
//                "name": "$name",
//                "phone": "$phone",
//                "email": "$email",
//                "country": "$country",
//                "tax_id": "${taxId ?: "null"}",
//                "tags": "${tags?.joinToString(";")}"
//            }
//        """.trimIndent()
//
//    companion object {
//        fun fromMap(map: Map<String, JsonElement>): Customer {
//            return Customer(
//                id = map["id"]?.jsonPrimitive?.intOrNull,
//                name = map["name"]!!.jsonPrimitive.content,
//                phone = map["phone"]!!.jsonPrimitive.content,
//                email = map["email"]!!.jsonPrimitive.content,
//                country = map["country"]!!.jsonPrimitive.content,
//                taxId = map["tax_id"]?.jsonPrimitive?.content,
//                tags = map["tags"]?.jsonPrimitive?.content?.split(";").orEmpty()
//            )
//        }
//    }
//}