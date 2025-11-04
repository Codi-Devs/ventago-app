package com.teco.ventago.features.business.domain.model.responses

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive

data class ReservationsConfig(
    var reservationMinTime: Int,
    var maxReservationsPerDay: Int,
    var maxCommensalsPerTable: Int,
    var reservationMaxFutureDays: Int,
    var allowReservations: Boolean,
) {

    constructor(response: JsonObject)
            : this(
        response["reservation_min_time_mins"]?.jsonPrimitive?.int ?: 0,
        response["max_reservations_per_day"]?.jsonPrimitive?.int ?: 0,
        response["reservations_max_comensals_per_table"]?.jsonPrimitive?.int ?: 0,
        response["reservation_max_future_days"]?.jsonPrimitive?.int ?: 0,
        (response["allow_reservations"]?.jsonPrimitive?.int ?: 0) == 1
    )


    val asJSONObject: JsonObject?
        get() {
            var config: JsonObject?
            val data = mutableMapOf<String, JsonElement>()
            try {
                data["reservation_min_time_mins"] = JsonPrimitive(reservationMinTime)
                data["max_reservations_per_day"] = JsonPrimitive(maxReservationsPerDay)
                data["reservations_max_comensals_per_table"] = JsonPrimitive(maxCommensalsPerTable)
                data["reservation_max_future_days"] = JsonPrimitive(reservationMaxFutureDays)
                data["allow_reservations"] = if (allowReservations) JsonPrimitive(1) else JsonPrimitive(0)
                config = JsonObject(data)
            } catch (e: Exception) {
                e.printStackTrace()
                config = null
            }
            return config
        }


    val configAsJson: JsonObject?
        get() {
            var config: JsonObject?
            val data = mutableMapOf<String, JsonElement>()
            try {
                data["reservationMinTime"] = JsonPrimitive(reservationMinTime)
                data["maxReservationsPerDay"] = JsonPrimitive(maxReservationsPerDay)
                data["maxCommensalsPerTable"] = JsonPrimitive(maxCommensalsPerTable)
                data["reservationMaxFutureDays"] = JsonPrimitive(reservationMaxFutureDays)
                data["allowReservations"] = JsonPrimitive(allowReservations)
                config = JsonObject(data)
            } catch (e: Exception) {
                e.printStackTrace()
                config = null
            }
            return config
        }
}