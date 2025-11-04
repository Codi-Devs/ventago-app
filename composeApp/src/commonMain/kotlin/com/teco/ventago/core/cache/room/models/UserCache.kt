package com.teco.ventago.core.cache.room.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.teco.ventago.features.auth.domain.model.response.BusinessIds
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity
data class UserCache (
    @PrimaryKey(autoGenerate = true)
    val id: Long = 1L,
    var uid: String,
    var email: String,
    var name: String,
    var premium: Boolean ,
    var active: Boolean,
    var missingBusiness: Boolean ,
    var userId: Int,
    var businessIds: BusinessIdsList,
)


fun List<BusinessIds>.toBusinessIdsList(): BusinessIdsList {
    return BusinessIdsList(
        this.map {
            BusinessIdsCache(
                businessId = it.businessId,
                menuId = it.menuId
            )
        }
    )
}

data class BusinessIdsList(
    val businessIds: List<BusinessIdsCache>
)

@Entity
data class BusinessIdsCache(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 1L,
    var businessId: Int = -1,
    var menuId: Int = -1,
)


