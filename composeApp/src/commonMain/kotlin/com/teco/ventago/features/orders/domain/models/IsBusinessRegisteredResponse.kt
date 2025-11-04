package com.teco.ventago.features.orders.domain.models

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive

data class IsBusinessRegisteredResponse(val isRegistered: Boolean, val ordersEnabled: Boolean, val ordersBanned: Boolean) {
    companion object {
        fun fromMap(map: Map<String, JsonElement>): IsBusinessRegisteredResponse {
            return IsBusinessRegisteredResponse(
                map["business_registered"]!!.jsonPrimitive.boolean,
                map["orders_enabled"]!!.jsonPrimitive.boolean,
                map["business_banned"]!!.jsonPrimitive.boolean
            )
        }
    }
}
