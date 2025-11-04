package com.teco.ventago.utils

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

fun List<RequestClass>.toRequestJson(): String {
    var json = "[";
    for (item in this) {
        json += if (this[this.size -1] == item) {
            item.toJson()+""
        } else {
            item.toJson()+","
        }
    }
    json += "]"
    return json.trimIndent()
}

abstract class RequestClass {
    abstract fun toJson(): String
}
