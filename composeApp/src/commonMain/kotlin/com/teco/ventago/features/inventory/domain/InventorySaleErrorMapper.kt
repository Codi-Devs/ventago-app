package com.teco.ventago.features.inventory.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object InventorySaleErrorMapper {
    const val ZERO_STOCK_CART_MESSAGE =
        "Este producto no tiene stock en inventario y no se puede facturar. Para facturar necesita stock o permitir ventas en inventario negativo."

    const val INSUFFICIENT_STOCK =
        "Uno o más productos no tienen stock suficiente y el negocio no permite ventas con inventario negativo. Agrega stock o permite ventas en negativo para facturar."
    const val LOCATION_MISSING =
        "No hay una ubicación de inventario configurada para esta sucursal y punto de facturación."
    const val RESERVATION_CONFLICT =
        "El inventario cambió mientras se creaba la orden. Inténtalo de nuevo."
    const val UNAVAILABLE =
        "El inventario no está disponible en este momento. Inténtalo de nuevo."
    const val REJECTED =
        "No se pudo reservar el inventario para esta venta. Revisa el stock o permite ventas en inventario negativo."

    private val json = Json { ignoreUnknownKeys = true }

    fun messageForCode(code: String?): String? = when (code) {
        "INV_STK_001" -> INSUFFICIENT_STOCK
        "INV_STK_002" -> LOCATION_MISSING
        "INV_STK_003" -> RESERVATION_CONFLICT
        "INV_STK_004" -> UNAVAILABLE
        "INV_STK_005" -> REJECTED
        else -> null
    }

    fun messageFor(error: Throwable, fallback: String): String {
        val parsed = parse(error.message.orEmpty())
        return messageForCode(parsed) ?: fallback
    }

    fun extractCode(raw: String): String? = parse(raw)

    private fun parse(raw: String): String? {
        if (raw.isBlank()) return null
        messageForCode(raw.trim())?.let { return raw.trim() }
        val payload = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull() ?: return null
        val code = payload["error"]?.jsonPrimitive?.contentOrNull
            ?: payload["errorCode"]?.jsonPrimitive?.contentOrNull
            ?: payload["code"]?.jsonPrimitive?.contentOrNull
        return code
    }
}
