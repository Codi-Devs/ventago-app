package com.teco.ventago.features.notifications.domain.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

private val metadataJson = Json { ignoreUnknownKeys = true }

object NotificationMetadataSerializer : KSerializer<JsonObject> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("NotificationMetadata")

    override fun deserialize(decoder: Decoder): JsonObject {
        val jsonDecoder = decoder as? JsonDecoder ?: return JsonObject(emptyMap())
        return decodeMetadata(jsonDecoder.decodeJsonElement())
    }

    override fun serialize(encoder: Encoder, value: JsonObject) {
        encoder.encodeSerializableValue(JsonObject.serializer(), value)
    }

    fun decodeMetadata(element: JsonElement): JsonObject {
        return when (element) {
            is JsonObject -> element
            is JsonPrimitive -> decodeFromString(element.content)
            is JsonArray -> decodeFromByteArray(element) ?: decodeFromString(element.toString())
            JsonNull -> JsonObject(emptyMap())
        }
    }

    private fun decodeFromByteArray(element: JsonArray): JsonObject? {
        if (element.isEmpty()) return JsonObject(emptyMap())
        val bytes = ByteArray(element.size)
        element.forEachIndexed { index, item ->
            val code = (item as? JsonPrimitive)?.intOrNull ?: return null
            if (code !in 0..255) return null
            bytes[index] = code.toByte()
        }
        return decodeFromString(bytes.decodeToString())
    }

    private fun decodeFromString(raw: String): JsonObject {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return JsonObject(emptyMap())
        return runCatching { metadataJson.parseToJsonElement(trimmed) }
            .map { parsed ->
                when (parsed) {
                    is JsonObject -> parsed
                    is JsonArray -> JsonObject(emptyMap())
                    else -> JsonObject(emptyMap())
                }
            }
            .getOrDefault(JsonObject(emptyMap()))
    }
}
