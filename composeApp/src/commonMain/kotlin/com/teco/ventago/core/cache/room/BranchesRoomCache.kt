package com.teco.ventago.core.cache.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.teco.ventago.core.cache.room.models.BranchesCache


@Dao
interface BranchesCacheDao {
    @Query("SELECT * FROM BranchesCache limit 1")
    suspend fun getBranches(): BranchesCache?

    @Insert
    suspend fun insert(branches: BranchesCache)

    @Query("DELETE FROM BranchesCache")
    suspend fun deleteBranches()
}