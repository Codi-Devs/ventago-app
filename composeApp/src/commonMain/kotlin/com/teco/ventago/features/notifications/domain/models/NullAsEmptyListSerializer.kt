package com.teco.ventago.features.notifications.domain.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull

open class NullAsEmptyListSerializer<T>(
    elementSerializer: KSerializer<T>,
) : KSerializer<List<T>> {
    private val listSerializer = ListSerializer(elementSerializer)
    override val descriptor: SerialDescriptor = listSerializer.descriptor

    override fun deserialize(decoder: Decoder): List<T> {
        val jsonDecoder = decoder as? JsonDecoder ?: return listSerializer.deserialize(decoder)
        return when (val element = jsonDecoder.decodeJsonElement()) {
            JsonNull -> emptyList()
            is JsonArray -> jsonDecoder.json.decodeFromJsonElement(listSerializer, element)
            else -> emptyList()
        }
    }

    override fun serialize(encoder: Encoder, value: List<T>) {
        listSerializer.serialize(encoder, value)
    }
}

class NotificationItemsSerializer : NullAsEmptyListSerializer<InAppNotification>(
    InAppNotification.serializer()
)
