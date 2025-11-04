package com.teco.ventago.core.cache.room.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.teco.ventago.features.branches.domain.model.Branch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity
data class BranchesCache(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 1L,
    val branchesJson: String,
)

fun BranchesCache.toObject(): List<Branch> {
    if (this.branchesJson.isEmpty()) return emptyList()
    return Json.decodeFromString<List<Branch>>(branchesJson)
}

fun List<Branch>.toCache(): BranchesCache {
    val jsonString = Json.encodeToString(this)
    return BranchesCache(
        id = 1L,
        branchesJson = jsonString
    )
}