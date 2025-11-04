package com.teco.ventago.core.cache.room.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.teco.ventago.features.business.domain.model.Business
import kotlinx.serialization.json.Json

@Entity
data class BusinessCache (
    @PrimaryKey(autoGenerate = true)
    val id: Long = 1L,
    var businessJson: String,
)

fun Business.toCache(): BusinessCache? {
    val jsonObject = this.asJSONObject
    return jsonObject?.let {
        BusinessCache(
            id = 1L,
            businessJson = jsonObject.toString()
        )
    }
}

fun BusinessCache.toObject(): Business {
    return Business(Json.decodeFromString(this.businessJson))
}