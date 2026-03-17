package com.teco.ventago.core.authz

import com.teco.ventago.utils.base64.base64Decoded
import com.teco.ventago.utils.base64.base64UrlDecoded
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class UserAuthzClaims(
    val scopes: Set<String>,
    val isSubUser: Boolean,
    val isOwnerMain: Boolean,
    val mustChangePassword: Boolean,
)

object AuthzJwtDecoder {
    fun decode(token: String?): UserAuthzClaims? {
        if (token.isNullOrBlank()) return null
        val parts = token.split(".")
        if (parts.size < 2) return null

        val payload = runCatching { parts[1].base64UrlDecoded }
            .getOrElse { runCatching { parts[1].base64Decoded }.getOrNull() }
            ?: return null

        val jsonObject = runCatching { Json.parseToJsonElement(payload).jsonObject }.getOrNull()
            ?: return null

        val dataObject = jsonObject["data"]?.jsonObject
        val topLevelScopes = jsonObject["scopes"].asStringSet()
        val nestedScopes = dataObject?.get("scopes").asStringSet()
        val scopes = (topLevelScopes + nestedScopes).toSet()

        val isSubUser = jsonObject["is_sub_user"]?.jsonPrimitive?.booleanOrNull
            ?: dataObject?.get("is_sub_user")?.jsonPrimitive?.booleanOrNull
            ?: false
        val target = jsonObject["target"]?.jsonPrimitive?.contentOrNull
            ?: dataObject?.get("target")?.jsonPrimitive?.contentOrNull
            ?: ""
        val mustChangePassword = parseBooleanFlag(jsonObject["must_change_password"])
            || parseBooleanFlag(dataObject?.get("must_change_password"))

        return UserAuthzClaims(
            scopes = scopes,
            isSubUser = isSubUser,
            isOwnerMain = !isSubUser && target == "businessOwner",
            mustChangePassword = mustChangePassword
        )
    }

    private fun parseBooleanFlag(value: JsonElement?): Boolean {
        val primitive = value?.jsonPrimitive ?: return false
        return primitive.booleanOrNull
            ?: primitive.contentOrNull?.trim()?.lowercase()?.let {
                it == "true" || it == "1" || it == "yes"
            }
            ?: false
    }

    private fun JsonArray?.toStringSet(): Set<String> {
        return this.orEmpty()
            .mapNotNull { it.jsonPrimitive.contentOrNull?.trim()?.takeIf(String::isNotBlank) }
            .toSet()
    }

    private fun Any?.asStringSet(): Set<String> {
        return when (this) {
            is JsonArray -> toStringSet()
            is JsonObject -> emptySet()
            else -> emptySet()
        }
    }
}
