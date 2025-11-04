package com.teco.ventago.core.cache.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.teco.ventago.core.cache.room.models.FinancialProfileCache

@Dao
interface FinancialProfileCacheDao {
    @Query("SELECT * FROM FinancialProfileCache limit 1")
    suspend fun getProfile(): FinancialProfileCache?

    @Insert
    suspend fun insert(cache: FinancialProfileCache)

    @Query("DELETE FROM FinancialProfileCache")
    suspend fun deleteProfile()
}
