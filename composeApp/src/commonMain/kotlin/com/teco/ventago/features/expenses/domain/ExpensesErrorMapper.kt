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

    private fun ApiResponse.matchesAccountNotFound(): Boolean {
        val code = errorCode?.lowercase()
        val message = errorMessage?.lowercase()
        return code == "expense account not found" || message == "expense account not found"
    }
}
