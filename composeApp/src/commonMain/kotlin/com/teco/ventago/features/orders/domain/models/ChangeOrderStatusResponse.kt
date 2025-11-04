package com.teco.ventago.features.orders.domain.models

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive

data class ChangeOrderStatusResponse(val changed: Boolean, val statusId: Int, val changedAt: String){
    companion object {
        fun fromMap(map: Map<String, JsonElement>): ChangeOrderStatusResponse {
            return ChangeOrderStatusResponse(
                map["changed"]!!.jsonPrimitive.boolean,
                map["status_id"]!!.jsonPrimitive.int,
                map["changed_at"]!!.jsonPrimitive.content
            )
        }
    }
}