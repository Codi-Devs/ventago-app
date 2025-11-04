package com.teco.ventago.features.business.domain.model

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonPrimitive

data class BusinessAddress(
    val placeId: String,
    val placeAddress: String,
    val placeLat: Double,
    val placeLng: Double,
) {

    constructor(response: JsonObject) : this(
        response["place_id"]?.jsonPrimitive?.content ?: "",
        response["place_address"]?.jsonPrimitive?.content ?: "",
        if (response["place_lat"]?.jsonPrimitive?.content == "null") {
            -1.0
        } else {
            response["place_lat"]?.jsonPrimitive?.double ?: -1.0
        },
        if (response["place_lon"]?.jsonPrimitive?.content == "null") {
            -1.0
        } else {
            response["place_lon"]?.jsonPrimitive?.double ?: 0.0
        },
    )

    val asJSONObject: JsonObject?
        get() {
            var address: JsonObject?
            val data = mutableMapOf<String, JsonElement>()
            try {
                data["place_id"] = JsonPrimitive(placeId)
                data["place_address"] = JsonPrimitive(placeAddress)
                data["place_lat"] = JsonPrimitive(placeLat)
                data["place_lon"] = JsonPrimitive(placeLng)
                address = JsonObject(data)
            } catch (e: Exception) {
                e.printStackTrace()
                address = null
            }
            return address
        }

}