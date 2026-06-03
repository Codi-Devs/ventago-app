package com.teco.ventago.features.branches.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class Branch (
    val branchCode: String,
    val name: String,
    val addressLine: String,
    val locationCode: String,
    val longitude : String,
    val latitude : String,
    val status: Int,
    val fiscalBillingPoints: List<FiscalBillingPoint>,
    val tradeName: String? = null,
    val logoUrl: String? = null,
){

    constructor(response: JsonObject) : this(
        response["code"]?.jsonPrimitive?.contentOrNull ?: "",
        response["name"]?.jsonPrimitive?.contentOrNull ?: "",
        response["address_line"]?.jsonPrimitive?.contentOrNull ?: "",
        response["location_code"]?.jsonPrimitive?.contentOrNull ?: "",
        response["longitude"]?.jsonPrimitive?.contentOrNull ?: "",
        response["latitude"]?.jsonPrimitive?.contentOrNull ?: "",
        response["status"]?.jsonPrimitive?.intOrNull ?: 0,
        FiscalBillingPoint.listFromMap(response["billing_points"]?.jsonArray ?: JsonArray(emptyList())),
        response["trade_name"]?.jsonPrimitive?.contentOrNull,
        response["logo_url"]?.jsonPrimitive?.contentOrNull,
    )

    val asJSONObject: JsonObject?
        get() {
            var jsonObject: JsonObject?
            val data = mutableMapOf<String, JsonElement>()
            try {
                data["code"] = JsonPrimitive(branchCode)
                data["name"] = JsonPrimitive(name)
                tradeName?.let { data["trade_name"] = JsonPrimitive(it) }
                data["address_line"] = JsonPrimitive(addressLine)
                data["location_code"] = JsonPrimitive(locationCode)
                data["longitude"] = JsonPrimitive(longitude)
                data["latitude"] = JsonPrimitive(latitude)
                logoUrl?.let { data["logo_url"] = JsonPrimitive(it) }
                data["status"] = JsonPrimitive(status)
                data["billing_points"] = JsonArray(fiscalBillingPoints.mapNotNull { it.asJSONObject })
                jsonObject = JsonObject(data)
            } catch (e: Exception) {
                e.printStackTrace()
                jsonObject = null
            }
            return jsonObject
        }


    companion object {
        fun listFromMap(arr: JsonArray): List<Branch> {
            val list = mutableListOf<Branch>()
            for (item in arr) {
                val map = Json.decodeFromJsonElement<Map<String, JsonElement>>(item)
                val element = Branch(JsonObject(map))
                list.add(element)
            }
            return list
        }
    }
}
