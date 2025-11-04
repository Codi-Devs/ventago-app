package com.teco.ventago.core.cache.room

import androidx.room.TypeConverter
import com.teco.ventago.core.cache.room.models.BusinessIdsCache
import com.teco.ventago.core.cache.room.models.BusinessIdsList
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull

class RoomTypeConverters{
    @TypeConverter
    fun convertBusinessIdsListToJSONString(list: BusinessIdsList): String {
        val jsonArray = list.businessIds.map {
            JsonObject(mapOf(
                "id" to JsonPrimitive(it.id),
                "businesId" to JsonPrimitive(it.businessId),
                "menuId" to JsonPrimitive(it.menuId)))
        }
        val jsonObject = JsonObject(mapOf("businessIds" to JsonArray(jsonArray)))
        return jsonObject.toString()
    }

    @TypeConverter
    fun convertJSONStringToBusinessIdsList(jsonString: String): BusinessIdsList {
        val jsonObject = Json.parseToJsonElement(jsonString).jsonObject
        val businessIdsArray = jsonObject["businessIds"]!!.jsonArray

        val businessIds = businessIdsArray.map {
            val obj = it.jsonObject
            BusinessIdsCache(
                obj["id"]!!.jsonPrimitive.longOrNull?:0,
                obj["businesId"]!!.jsonPrimitive.intOrNull?:0,
                obj["menuId"]!!.jsonPrimitive.intOrNull?:0,
            )
        }

        return BusinessIdsList(businessIds)
    }

}