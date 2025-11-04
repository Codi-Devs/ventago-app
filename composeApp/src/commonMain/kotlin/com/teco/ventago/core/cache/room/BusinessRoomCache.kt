package com.teco.ventago.core.cache.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.teco.ventago.core.cache.room.models.BusinessCache

@Dao
interface BusinessCacheDao {
    @Query("SELECT * FROM BusinessCache limit 1")
    suspend fun getBusiness(): BusinessCache?

    @Insert
    suspend fun insert(user: BusinessCache)

    @Query("DELETE FROM BusinessCache")
    suspend fun deleteBusiness()
}
