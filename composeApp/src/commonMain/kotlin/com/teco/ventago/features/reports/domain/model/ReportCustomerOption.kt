package com.teco.ventago.features.reports.domain.model

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull

data class ReportCustomerOption(
    val id: Long,
    val name: String,
    val ruc: String,
) {
    val displayName: String = if (ruc.isBlank()) name else "$name - $ruc"

    companion object {
        fun listFromResponse(data: JsonObject): List<ReportCustomerOption> {
            val items = data["items"] as? JsonArray ?: return emptyList()
            return items.mapNotNull { element ->
                val item = element as? JsonObject ?: return@mapNotNull null
                val id = (item["id"] as? JsonPrimitive)?.longOrNull ?: return@mapNotNull null
                val name = (item["name"] as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()
                if (name.isBlank()) return@mapNotNull null
                ReportCustomerOption(
                    id = id,
                    name = name,
                    ruc = (item["ruc"] as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()
                )
            }
        }
    }
}
