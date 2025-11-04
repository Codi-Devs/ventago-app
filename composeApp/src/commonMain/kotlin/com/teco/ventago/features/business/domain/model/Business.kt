package com.teco.ventago.features.business.domain.model

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class Business(
    val businessId: Int,
    var name: String,
    val description: String,
    val active: Boolean,
    val currency: Currency,
    var logo: String,
    var socialNetwork: BusinessSocialNetwork,
    var phone: String,
    var address: BusinessAddress,
    val domain: String,
    val isFull: Boolean,
    val ruc: String?,
    val web: String?,
    val businessEmail: String?,
) {

    constructor(response: JsonObject) : this(
        response["id_bussiness"]?.jsonPrimitive?.int ?: -1,
        response["name"]?.jsonPrimitive?.content ?: "",
        response["desc"]?.jsonPrimitive?.content ?: "",
        (response["active"]?.jsonPrimitive?.int ?: 0) == 1,
        if (response.containsKey("currency")) Currency.fromJSON(response["currency"]!!.jsonObject)
        else Currency.fromId(response["currency_id"]?.jsonPrimitive?.int ?: 1),
        response["logo"]?.jsonPrimitive?.content ?: "",
        BusinessSocialNetwork(response["social_network"]?.jsonObject ?: JsonObject(emptyMap())),
        response["phone"]?.jsonPrimitive?.content ?: "",
        BusinessAddress(response["address"]?.jsonObject ?: JsonObject(emptyMap())),
        response["domain"]?.jsonPrimitive?.content ?: "",
        (response["full_reservations"]?.jsonPrimitive?.int ?: 0) == 1,
        response["ruc"]?.jsonPrimitive?.content,
        response["web"]?.jsonPrimitive?.content,
        response["business_email"]?.jsonPrimitive?.content,
    )


    val isAddressEmpty: Boolean
        get() {
            var empty = false
            if (address.placeAddress.trim { it <= ' ' }
                    .isEmpty() || address.placeAddress.equals("null", ignoreCase = true)) {
                empty = true
            }
            return empty
        }

    val isPhoneEmpty: Boolean
        get() {
            var empty = false
            if (phone.trim { it <= ' ' }.isEmpty()) {
                empty = true
            }
            return empty
        }

    val isSocialNetworksEmpty: Boolean
        get() = socialNetwork.isEmptySocialNetworks()

    val asJSONObject: JsonObject?
        get() {
            var business: JsonObject?
            val data = mutableMapOf<String, JsonElement>()
            try {
                data["id_bussiness"] = JsonPrimitive(businessId)
                data["name"] = JsonPrimitive(name)
                data["desc"] = JsonPrimitive(description)
                data["active"] = JsonPrimitive(if (active) 1 else 0)
                data["currency"] = currency.toJSON()
                data["logo"] = JsonPrimitive(logo)
                data["social_network"] = socialNetwork.asJSONObject ?: JsonObject(emptyMap())
                data["phone"] = JsonPrimitive(phone)
                data["address"] = address.asJSONObject ?: JsonObject(emptyMap())
                data["domain"] = JsonPrimitive(domain)
                data["full_reservations"] = JsonPrimitive(if (isFull) 1 else 0)
                ruc?.let { data["ruc"] = JsonPrimitive(it) }
                web?.let { data["web"] = JsonPrimitive(it) }
                businessEmail?.let { data["business_email"] = JsonPrimitive(it) }
                business = JsonObject(data)
            } catch (e: Exception) {
                e.printStackTrace()
                business = null
            }
            return business
        }

}