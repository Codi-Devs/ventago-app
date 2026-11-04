package com.teco.ventago.features.product.domain.model

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull

internal fun Map<String, JsonElement>.isRemovedFromCatalog(): Boolean {
    return isDeletedCatalogFlag(this["deleted"]) || isDeletedCatalogFlag(this["deleted_at"])
}

private fun isDeletedCatalogFlag(flag: JsonElement?): Boolean {
    if (flag == null || flag is JsonNull) return false
    val primitive = flag as? JsonPrimitive ?: return false
    if (primitive.isString) {
        val value = primitive.content.trim()
        return value.isNotEmpty() && value != "0" && !value.equals("false", ignoreCase = true)
    }
    return primitive.booleanOrNull == true || primitive.intOrNull == 1
}
