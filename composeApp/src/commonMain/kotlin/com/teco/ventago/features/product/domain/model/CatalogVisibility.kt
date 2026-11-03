package com.teco.ventago.features.product.domain.model

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

internal fun Map<String, JsonElement>.isRemovedFromCatalog(): Boolean {
    val flag = this["deleted"] ?: this["deleted_at"] ?: return false
    val primitive = flag.jsonPrimitive
    if (primitive.isString) {
        val value = primitive.content.trim()
        return value.isNotEmpty() && value != "0" && !value.equals("false", ignoreCase = true)
    }
    return primitive.booleanOrNull == true || primitive.intOrNull == 1
}
