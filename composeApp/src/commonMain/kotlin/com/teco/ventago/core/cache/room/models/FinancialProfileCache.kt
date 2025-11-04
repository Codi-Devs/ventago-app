package com.teco.ventago.core.cache.room.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.teco.ventago.features.financialProfile.domain.model.BusinessFinancialProfile
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val json = Json {
    ignoreUnknownKeys = true // Optional: skip unknown fields
}

@Entity
data class FinancialProfileCache (
    @PrimaryKey(autoGenerate = true)
    val id: Long = 1L,
    var profileJson: String,
)

fun BusinessFinancialProfile.toCache(): FinancialProfileCache? {
    val jsonObject = json.encodeToString(this)
    return FinancialProfileCache(
        id = 1L,
        profileJson = jsonObject
    )
}

fun FinancialProfileCache.toObject(): BusinessFinancialProfile {
    return json.decodeFromString<BusinessFinancialProfile>(this.profileJson)
}