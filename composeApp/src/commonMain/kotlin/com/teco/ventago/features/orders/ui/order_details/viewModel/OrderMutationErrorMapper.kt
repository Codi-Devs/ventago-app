package com.teco.ventago.features.orders.ui.order_details.viewModel

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal object OrderMutationErrorMapper {

    private val json = Json { ignoreUnknownKeys = true }

    fun messageFor(
        error: Throwable,
        fallback: String,
        codeOverrides: Map<String, String> = emptyMap()
    ): String {
        val parsed = parse(error.message.orEmpty())
        val backendMessage = parsed.backendMessage
        if (!backendMessage.isNullOrBlank() && backendMessage != parsed.code) {
            return backendMessage
        }

        if (parsed.code == "O_RP_005" && parsed.action == "manual_refund_required") {
            return "El proveedor ya confirmó el pago o el estado cambió. No se puede cambiar el método; requiere reembolso o conciliación manual."
        }

        return when (parsed.code) {
            "O_RP_001" -> codeOverrides["O_RP_001"] ?: "No tienes permisos para realizar esta accion."
            "O_RP_002" -> "La suma de los nuevos vencimientos debe coincidir exactamente con el saldo abierto total."
            "O_RP_004" -> "No se encontro la orden o pago solicitado."
            "O_RP_005" -> "No se puede completar la accion por el estado actual del recurso."
            "PAY_001" -> "No fue posible procesar la operacion de pago."
            "PAY_002" -> "No hay metodos de pago configurados para continuar."
            "PAY_PP_001" -> "No fue posible completar la operacion con PayPal."
            "INV_001" -> "No fue posible emitir la factura en este momento."
            "INV_002" -> "La factura no esta disponible para esta operacion."
            "INV_003" -> "El motivo de anulación debe tener al menos 15 caracteres"
            "INV_004" -> "La factura no se puede anular todavía porque aún no está disponible en la DGI"
            null, "", "null" -> fallback
            else -> "$fallback Código: ${parsed.code}"
        }
    }

    private fun parse(raw: String): ParsedOrderMutationError {
        val payload = runCatching {
            json.parseToJsonElement(raw).jsonObject
        }.getOrNull()

        val code = payload?.get("error")?.jsonPrimitive?.contentOrNull
            ?: payload?.get("errorCode")?.jsonPrimitive?.contentOrNull

        val backendMessage = runCatching {
            payload?.get("data")
                ?.jsonObject
                ?.get("message")
                ?.jsonPrimitive
                ?.contentOrNull
        }.getOrNull()
            ?: payload?.get("errorMessage")?.jsonPrimitive?.contentOrNull

        val action = runCatching {
            payload?.get("data")
                ?.jsonObject
                ?.get("action")
                ?.jsonPrimitive
                ?.contentOrNull
        }.getOrNull()

        return ParsedOrderMutationError(
            code = code,
            backendMessage = backendMessage,
            action = action
        )
    }
}

private data class ParsedOrderMutationError(
    val code: String? = null,
    val backendMessage: String? = null,
    val action: String? = null
)
