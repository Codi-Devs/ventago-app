@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.teco.ventago.features.customers.domain.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

@Serializable
data class CustomerTaxRetentionOption(
    val code: String,
    val label: String,
    val defaultRate: Int? = null,
)

object CustomerTaxRetentionCatalog {
    val options: List<CustomerTaxRetentionOption> = listOf(
        CustomerTaxRetentionOption("", "Sin retencion"),
        CustomerTaxRetentionOption("1", "Pago por servicio profesional al estado 100%", 100),
        CustomerTaxRetentionOption("2", "Pago por venta de bienes/servicios al estado 50%", 50),
        CustomerTaxRetentionOption("3", "Pago o acreditacion a no domiciliado o empresa constituida en el exterior 100%", 100),
        CustomerTaxRetentionOption("4", "Pago o acreditacion por compra de bienes/servicios 50%", 50),
        CustomerTaxRetentionOption("7", "Pago a comercio afiliado a sistema de TC/TD 50%", 50),
        CustomerTaxRetentionOption("8", "Otros (disminucion de la retencion)"),
    )

    fun normalizeCode(value: String?): String {
        val raw = value?.trim().orEmpty()
        if (raw.isEmpty()) return ""
        return raw.toIntOrNull()?.toString() ?: raw
    }

    fun normalizeCode(value: Int?): String = normalizeCode(value?.toString())

    fun indexOfCode(value: String?): Int {
        val normalized = normalizeCode(value)
        val index = options.indexOfFirst { it.code == normalized }
        return if (index >= 0) index else 0
    }

    fun indexOfCode(value: Int?): Int = indexOfCode(value?.toString())

    fun optionForCode(value: String?): CustomerTaxRetentionOption? {
        val normalized = normalizeCode(value)
        if (normalized.isEmpty()) return options.first()
        return options.firstOrNull { it.code == normalized }
    }

    fun optionForCode(value: Int?): CustomerTaxRetentionOption? = optionForCode(value?.toString())

    fun defaultRateForCode(value: String?): Int? = optionForCode(value)?.defaultRate

    fun defaultRateForCode(value: Int?): Int? = defaultRateForCode(value?.toString())
}

object FlexibleBooleanSerializer : KSerializer<Boolean> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleBoolean", PrimitiveKind.BOOLEAN)

    override fun serialize(encoder: Encoder, value: Boolean) {
        encoder.encodeBoolean(value)
    }

    override fun deserialize(decoder: Decoder): Boolean {
        if (decoder !is JsonDecoder) {
            return decoder.decodeBoolean()
        }
        return parseBoolean(decoder.decodeJsonElement())
    }

    private fun parseBoolean(element: JsonElement): Boolean {
        if (element is JsonNull) return false
        val primitive = element as? JsonPrimitive
            ?: throw SerializationException("Unsupported boolean payload: $element")
        primitive.booleanOrNull?.let { return it }
        primitive.intOrNull?.let { return it != 0 }
        return when (primitive.contentOrNull?.trim()?.lowercase()) {
            "true", "1", "yes", "si" -> true
            "false", "0", "no", "" -> false
            null -> false
            else -> false
        }
    }
}

object FlexibleNullableIntSerializer : KSerializer<Int?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleNullableInt", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Int?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            encoder.encodeInt(value)
        }
    }

    override fun deserialize(decoder: Decoder): Int? {
        if (decoder !is JsonDecoder) {
            return decoder.decodeInt()
        }
        return parseNullableInt(decoder.decodeJsonElement())
    }

    private fun parseNullableInt(element: JsonElement): Int? {
        if (element is JsonNull) return null
        val primitive = element as? JsonPrimitive ?: return null
        primitive.intOrNull?.let { return it }
        return primitive.contentOrNull
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.toIntOrNull()
    }
}
