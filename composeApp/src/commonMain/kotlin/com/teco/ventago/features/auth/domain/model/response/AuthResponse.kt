package com.teco.ventago.features.auth.domain.model.response

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

data class AuthResponse(
    val uid: String,
    val email: String,
    val name: String,
    val premium: Boolean,
    val active: Boolean,
    val missingBusiness: Boolean,
    val userId: Int,
    val accessToken: String,
    val refreshToken: String,
    val providerToken: String,
    val message: String,
    val businesses: List<BusinessIds>,
    val activeBusinessId: Int = -1,
)  {
    companion object {
        fun fromMap(map: Map<String, JsonElement>): AuthResponse {
            var businesses = emptyList<BusinessIds>()
            if (map["business"] is JsonArray) {
                businesses = BusinessIds.listFromMap(
                    map["business"]?.jsonArray ?: JsonArray(emptyList()))
            }
            val activeBusinessId = map["business_id"]?.jsonPrimitive?.intOrNull ?: -1
            if (activeBusinessId > 0) {
                val selected = businesses.filter { it.businessId == activeBusinessId }
                val rest = businesses.filter { it.businessId != activeBusinessId }
                businesses = if (selected.isEmpty()) {
                    listOf(BusinessIds(activeBusinessId, -1)) + rest
                } else {
                    selected + rest
                }
            }

            return AuthResponse(
                map["uid"]?.jsonPrimitive?.contentOrNull ?: "",
                map["email"]?.jsonPrimitive?.contentOrNull ?: "",
                map["name"]?.jsonPrimitive?.contentOrNull ?: "",
                map["premium"]?.jsonPrimitive?.booleanOrNull ?: false,
                map["active"]?.jsonPrimitive?.booleanOrNull ?: false,
                map["missing_business"]?.jsonPrimitive?.booleanOrNull ?: false,
                map["user_id"]?.jsonPrimitive?.intOrNull ?: -1,
                map["access_token"]?.jsonPrimitive?.contentOrNull ?: "",
                map["refresh_token"]?.jsonPrimitive?.contentOrNull ?: "",
                map["token"]?.jsonPrimitive?.contentOrNull ?: "",
                map["message"]?.jsonPrimitive?.contentOrNull ?: "",
                businesses,
                activeBusinessId
            )
        }
    }
}


data class BusinessIds(
    val businessId: Int,
    val menuId: Int,
    val name: String = "",
    val role: String = "",
    val logo: String = "",
) {
    companion object {
        fun fromMap(map: Map<String, JsonElement>): BusinessIds {
            return BusinessIds(
                map["id"]?.jsonPrimitive?.intOrNull ?: -1,
                map["id_menu"]?.jsonPrimitive?.intOrNull ?: -1,
                map["name"]?.jsonPrimitive?.contentOrNull ?: "",
                map["role"]?.jsonPrimitive?.contentOrNull ?: "",
                map["logo"]?.jsonPrimitive?.contentOrNull ?: "",
            )
        }

        fun listFromMap(arr: JsonArray): List<BusinessIds> {
            val list = mutableListOf<BusinessIds>()
            for (item in arr) {
                val map = Json.decodeFromJsonElement<Map<String, JsonElement>>(item)
                val businesses = fromMap(map)
                list.add(businesses)
            }
            return list
        }
    }
}