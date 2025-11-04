package com.teco.ventago.features.auth.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

data class FreeTrial(
    var offerStartDate: String,
    var offerEndDate: String,
    var freeTrialAvailable: Boolean,
    var recovered: Boolean,
) {
    constructor(response: JsonObject) :this(
        offerStartDate = response["offerStartDate"]?.jsonPrimitive?.contentOrNull ?: "",
        offerEndDate = response["offerEndDate"]?.jsonPrimitive?.contentOrNull ?: "",
        freeTrialAvailable = response["freeTrialAvailable"]?.jsonPrimitive?.booleanOrNull ?: false,
        recovered = response["recovered"]?.jsonPrimitive?.booleanOrNull ?: false,
    )

    fun isExpired(): Boolean {
        return try {
            if (offerEndDate.isBlank()) return true
            val instant = Instant.parse(offerEndDate)
            val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            dateTime <= Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        } catch (e: Exception) {
            true
        }
    }
}