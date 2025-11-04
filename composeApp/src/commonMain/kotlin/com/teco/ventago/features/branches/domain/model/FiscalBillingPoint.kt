package com.teco.ventago.features.branches.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class FiscalBillingPoint(
    val billingPoint: String,
    val description: String?,
    val status: Int,
) {
    constructor(response: JsonObject) : this(
        response["code"]?.jsonPrimitive?.contentOrNull ?: "",
        response["description"]?.jsonPrimitive?.contentOrNull,
        response["status"]?.jsonPrimitive?.intOrNull ?: 0
    )

    val asJSONObject: JsonObject?
        get() {
            var jsonObject: JsonObject?
            val data = mutableMapOf<String, JsonElement>()
            try {
                data["code"] = JsonPrimitive(billingPoint)
                data["description"] = if (description != null) { JsonPrimitive(description) } else { JsonNull }
                data["status"] = JsonPrimitive(status)
                jsonObject = JsonObject(data)
            } catch (e: Exception) {
                e.printStackTrace()
                jsonObject = null
            }
            return jsonObject
        }

    companion object {
        fun listFromMap(arr: JsonArray): List<FiscalBillingPoint> {
            val list = mutableListOf<FiscalBillingPoint>()
            for (item in arr) {
                val map = Json.decodeFromJsonElement<Map<String, JsonElement>>(item)
                val element = FiscalBillingPoint(JsonObject(map))
                list.add(element)
            }
            return list
        }
    }

}