package com.teco.ventago.features.expenses.domain

import com.teco.ventago.utils.ApiResponse

object ExpensesErrorMapper {

    fun mapCatalogError(response: ApiResponse): String {
        return when {
            response.errorCode == "O_RP_002" ->
                "La solicitud es invalida. Verifica los datos e intentalo nuevamente."
            response.matchesAccountNotFound() ->
                "La cuenta de gasto no fue encontrada."
            response.errorCode == "AUTH_001" ->
                "Tu sesion expiro. Inicia sesion nuevamente."
            else -> "No se pudieron procesar los conceptos de gasto."
        }
    }

    fun mapCreateOrEditExpenseError(response: ApiResponse): String {
        return when {
            response.errorCode == "O_RP_002" ->
                "La informacion enviada es invalida. Verifica los datos del gasto."
            response.matchesAccountNotFound() ->
                "El concepto de gasto seleccionado no fue encontrado."
            response.errorCode == "AUTH_001" ->
                "Tu sesion expiro. Inicia sesion nuevamente."
            response.matchesPaymentExceedsTotal() ->
                "El monto registrado no puede superar el total del gasto."
            else -> "No se pudo guardar el gasto. Intentalo nuevamente."
        }
    }

    fun mapCategorizationError(response: ApiResponse): String {
        return when {
            response.errorCode == "O_RP_002" ->
                "La solicitud de conceptos es invalida."
            response.matchesAccountNotFound() ->
                "El concepto de gasto seleccionado no fue encontrado."
            response.errorCode == "AUTH_001" ->
                "Tu sesion expiro. Inicia sesion nuevamente."
            else -> "No se pudieron guardar los conceptos de gasto."
        }
    }

    fun mapMerchantError(response: ApiResponse): String {
        return when {
            response.matchesMerchantNotFound() ->
                "El proveedor no fue encontrado."
            response.matchesMerchantAlreadyExists() ->
                "Ya existe un proveedor con ese RUC."
            response.errorCode == "O_RP_002" ->
                "La informacion del proveedor es invalida. Verifica los datos."
            response.errorCode == "AUTH_001" ->
                "Tu sesion expiro. Inicia sesion nuevamente."
            else -> "No se pudo procesar la solicitud del proveedor."
        }
    }

    private fun ApiResponse.matchesPaymentExceedsTotal(): Boolean {
        val code = errorCode?.lowercase().orEmpty()
        val message = errorMessage?.lowercase().orEmpty()
        return code.contains("payment amount exceeds expense total") ||
            message.contains("payment amount exceeds expense total") ||
            code.contains("payment_exceeds_total") ||
            message.contains("payment_exceeds_total")
    }

    private fun ApiResponse.matchesAccountNotFound(): Boolean {
        val code = errorCode?.lowercase()
        val message = errorMessage?.lowercase()
        return code == "expense account not found" || message == "expense account not found"
    }

    private fun ApiResponse.matchesMerchantNotFound(): Boolean {
        val code = errorCode?.lowercase()
        val message = errorMessage?.lowercase()
        return code == "expense merchant not found" || message == "expense merchant not found"
    }

    private fun ApiResponse.matchesMerchantAlreadyExists(): Boolean {
        val code = errorCode?.lowercase()
        val message = errorMessage?.lowercase()
        return code == "expense merchant already exists" || message == "expense merchant already exists"
    }
}
