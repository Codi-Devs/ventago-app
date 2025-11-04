package com.teco.ventago.utils

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive

@kotlinx.serialization.Serializable
data class ApiResponse(val successful: Boolean, val data: JsonElement?, val error: ApiError?) {


    fun toJson(): String {
        return "{\"successful\":$successful,\"data\":${data?.toString() ?: "null"},\"error\":\"${error?.error}\"}"
    }
    companion object {
        fun fromJson(json: JsonObject): ApiResponse {
            val successful = json["success"]?.jsonPrimitive?.boolean ?: false
            val error = ApiError.fromError(json["error"]?.jsonPrimitive!!.content)
            val data = json["data"]
            return ApiResponse(successful, data, error)
        }
    }
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
    UNDEFINED("U_001"),
    INVALID_RUC("CU_001"),
    RUC_NOT_FOUND("CU_002"),
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
                else -> NO_ERROR
            }
        }
    }
}

fun ApiError?.isError(): Boolean {
    return this != ApiError.NO_ERROR
}


