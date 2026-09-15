package com.teco.ventago.features.pos.domain

import com.teco.ventago.features.orders.domain.models.requests.NameValue
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

fun cloneOrderLineAdditionalInfo(source: List<NameValue>?): JsonObject? {
    if (source.isNullOrEmpty()) {
        return null
    }
    val cloned = buildJsonObject {
        source.forEach { entry ->
            val name = entry.name.trim()
            if (name.isEmpty() || isBlankJsonValue(entry.value)) {
                return@forEach
            }
            put(name, entry.value)
        }
    }
    return cloned.takeIf { it.isNotEmpty() }
}

fun resolveCartAdditionalInfo(
    lineAdditionalInfo: List<NameValue>?,
    catalogAdditionalInfo: JsonObject?,
): JsonObject? {
    return cloneOrderLineAdditionalInfo(lineAdditionalInfo) ?: catalogAdditionalInfo
}

fun additionalInfoToNameValues(info: JsonObject?): List<NameValue> {
    if (info == null || info.isEmpty()) {
        return emptyList()
    }
    return info.map { (key, value) -> NameValue(name = key, value = value) }
}

private fun isBlankJsonValue(value: JsonElement): Boolean {
    val primitive = value as? JsonPrimitive ?: return false
    return primitive.contentOrNull?.trim().isNullOrEmpty()
}
