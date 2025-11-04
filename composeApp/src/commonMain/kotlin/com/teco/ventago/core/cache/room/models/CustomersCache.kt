package com.teco.ventago.core.cache.room.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.teco.ventago.core.Paged
import com.teco.ventago.features.customers.domain.models.CustomerListItem
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity
data class CustomerCache (
    @PrimaryKey(autoGenerate = true)
    val id: Long = 1L,
    var customersJson: String,
)

fun CustomerCache.toObject(): Paged<CustomerListItem> {
    if (this.customersJson.isEmpty()) return Paged(0, 0, 0, emptyList())
    return Json.decodeFromString<Paged<CustomerListItem>>(customersJson)
}

fun Paged<CustomerListItem>.toCache(): CustomerCache {
    val jsonString = Json.encodeToString(this)
    return CustomerCache(
        id = 1L,
        customersJson = jsonString
    )
}