package com.teco.ventago.utils

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.JsonNull

@kotlinx.serialization.Serializable
data class ApiResponse(
    val successful: Boolean,
    val data: JsonElement?,
    val error: ApiError?,
    val errorCode: String? = null,
    val errorMessage: String? = null
) {


    fun toJson(): String {
        return "{\"successful\":$successful,\"data\":${data?.toString() ?: "null"},\"error\":\"${errorCode ?: error?.error}\",\"errorMessage\":${errorMessage?.let { "\"$it\"" } ?: "null"}}"
    }
    companion object {
        fun fromJson(json: JsonObject): ApiResponse {
            val successful = json["success"]?.jsonPrimitive?.booleanOrNull ?: false
            val parsedError = parseError(json["error"])
            val data = json["data"]
            return ApiResponse(
                successful = successful,
                data = data,
                error = ApiError.fromError(parsedError.code),
                errorCode = parsedError.code,
                errorMessage = parsedError.message
            )
        }

        private fun parseError(errorElement: JsonElement?): ParsedError {
            return when (errorElement) {
                null, JsonNull -> ParsedError()
                is JsonPrimitive -> {
                    val content = errorElement.contentOrNull
                    ParsedError(
                        code = content?.takeIf { it.isNotBlank() },
                        message = content?.takeIf { it.isNotBlank() }
                    )
                }
                is JsonObject -> {
                    ParsedError(
                        code = errorElement["code"]?.jsonPrimitive?.contentOrNull,
                        message = errorElement["message"]?.jsonPrimitive?.contentOrNull
                    )
                }
                else -> ParsedError(message = errorElement.toString())
            }
        }
    }

    private data class ParsedError(
        val code: String? = null,
        val message: String? = null
    )
}

enum class ApiError(val error: String?){
    AUTH_001("AUTH_001"),
    AUTH_002("AUTH_002"),
    AUTH_003("AUTH_003"),
    AUTH_004("AUTH_004"),
    BR_001("BR_001"),
    BR_002("BR_002"),
    DB_001("DB_001"),
    BU_001("BU_001"), // Domain in use
    F_AUTH_001("F_AUTH_001"), // INVALID_PASSWORD, INVALID_EMAIL, MISSING_PASSWORD, OPERATION_NOT_ALLOWED, WEAK_PASSWORD
    F_AUTH_002("F_AUTH_002"), // EMAIL_NOT_FOUND
    F_AUTH_003("F_AUTH_003"), // USER_DISABLED
    F_AUTH_004("F_AUTH_004"), // EMAIL_EXISTS
    F_AUTH_005("F_AUTH_005"), // TOO_MANY_ATTEMPTS_TRY_LATER
    F_AUTH_006("F_AUTH_006"), // UNDEFINED
    F_AUTH_007("F_AUTH_007"), // Firebase auth expection
    O_RP_001("O_RP_001"),
    O_RP_002("O_RP_002"),
    O_RP_004("O_RP_004"),
    O_RP_005("O_RP_005"),
    PAY_001("PAY_001"),
    PAY_002("PAY_002"),
    PAY_PP_001("PAY_PP_001"),
    INV_001("INV_001"),
    INV_002("INV_002"),
    INV_003("INV_003"),
    INV_004("INV_004"),
    UNDEFINED("U_001"),
    INVALID_RUC("CU_001"),
    RUC_NOT_FOUND("CU_002"),
    CUSTOMER_ALREADY_EXISTS("CU_004"),
    NO_ERROR(null);

    companion object{
        fun fromError(error: String?) : ApiError {
            return when (error) {
                "AUTH_001" -> AUTH_001
                "AUTH_002" -> AUTH_002
                "AUTH_003" -> AUTH_003
                "AUTH_004" -> AUTH_004
                "BR_001" -> BR_001
                "BR_002" -> BR_002
                "DB_001" -> DB_001
                "BU_001" -> BU_001 // Domain in use
                "F_AUTH_001" -> F_AUTH_001   // INVALID_PASSWORD, INVALID_EMAIL, MISSING_PASSWORD, OPERATION_NOT_ALLOWED, WEAK_PASSWORD
                "F_AUTH_002" -> F_AUTH_002   // EMAIL_NOT_FOUND
                "F_AUTH_003" -> F_AUTH_003   // USER_DISABLED
                "F_AUTH_004" -> F_AUTH_004   // EMAIL_EXISTS
                "F_AUTH_005" -> F_AUTH_005   // TOO_MANY_ATTEMPTS_TRY_LATER
                "F_AUTH_006" -> F_AUTH_006   // UNDEFINED
                "F_AUTH_007" -> F_AUTH_007  // Firebase auth expection
                "O_RP_001" -> O_RP_001
                "O_RP_002" -> O_RP_002
                "O_RP_004" -> O_RP_004
                "O_RP_005" -> O_RP_005
                "PAY_001" -> PAY_001
                "PAY_002" -> PAY_002
                "PAY_PP_001" -> PAY_PP_001
                "INV_001" -> INV_001
                "INV_002" -> INV_002
                "INV_003" -> INV_003
                "INV_004" -> INV_004
                "CU_004" -> CUSTOMER_ALREADY_EXISTS
                else -> NO_ERROR
            }
        }
    }
}

fun ApiError?.isError(): Boolean {
    return this != ApiError.NO_ERROR
}
