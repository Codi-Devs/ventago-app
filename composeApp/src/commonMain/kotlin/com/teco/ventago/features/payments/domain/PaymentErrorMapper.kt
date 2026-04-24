package com.teco.ventago.features.payments.domain

object PaymentErrorMapper {

    fun messageForCode(code: String?, fallback: String): String {
        return when (code) {
            "O_RP_001" -> "No tienes permisos para realizar esta accion."
            "O_RP_002" -> "La solicitud es invalida. Verifica la informacion e intentalo nuevamente."
            "O_RP_004" -> "No se encontro el recurso solicitado."
            "O_RP_005" -> "El recurso ya no esta disponible para esta accion."
            "PAY_001" -> "No fue posible procesar la operacion de pago."
            "PAY_002" -> "El metodo de pago no esta configurado o no esta disponible."
            "PAY_PP_001" -> "No fue posible completar la operacion con PayPal."
            "INV_001" -> "No fue posible emitir la factura en este momento."
            "INV_002" -> "La factura no esta disponible para esta operacion."
            else -> fallback
        }
    }
}
