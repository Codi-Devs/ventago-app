package com.teco.ventago.features.business.domain.model

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

data class BusinessSocialNetwork(
    var facebook: String,
    var youtube: String,
    var instagram: String,
    var whatsapp: String,
) {

    constructor(response: JsonObject) : this(
        response["facebook_link"]?.jsonPrimitive?.content ?: "",
        response["youtube_link"]?.jsonPrimitive?.content ?: "",
        response["instagram_link"]?.jsonPrimitive?.content ?: "",
        response["whatsapp_number"]?.jsonPrimitive?.content ?: ""
    )


    fun isEmptySocialNetworks(): Boolean {
        return (facebook.trim { it <= ' ' }.isEmpty()
            && instagram.trim { it <= ' ' }.isEmpty()
            && youtube.trim { it <= ' ' }.isEmpty()
            && whatsapp.trim { it <= ' ' }.isEmpty()
        )
    }

    val asJSONObject: JsonObject?
        get() {
            var socialNetworks: JsonObject?
            val data = mutableMapOf<String, JsonElement>()
            try {
                data["facebook_link"] = JsonPrimitive(facebook)
                data["instagram_link"] = JsonPrimitive(instagram)
                data["youtube_link"] = JsonPrimitive(youtube)
                data["whatsapp_number"] = JsonPrimitive(whatsapp)
                socialNetworks = JsonObject(data)
            } catch (e: Exception) {
                e.printStackTrace()
                socialNetworks = null
            }
            return socialNetworks
        }

    val socialNetworksAsJson: JsonObject?
        get() {
            var socialNetworks: JsonObject?
            val data = mutableMapOf<String, JsonElement>()
            try {
                data["facebook"] = JsonPrimitive(facebook)
                data["instagram"] = JsonPrimitive(instagram)
                data["youtube"] = JsonPrimitive(youtube)
                data["whatsapp"] = JsonPrimitive(whatsapp)
                socialNetworks = JsonObject(data)
            } catch (e: Exception) {
                e.printStackTrace()
                socialNetworks = null
            }
            return socialNetworks
        }
}