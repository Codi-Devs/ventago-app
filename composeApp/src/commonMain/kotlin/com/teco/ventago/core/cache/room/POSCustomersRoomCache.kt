package com.teco.ventago.core.cache.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.teco.ventago.core.cache.room.models.CustomerCache

@Dao
interface CustomersCacheDao {
    @Query("SELECT * FROM CustomerCache limit 1")
    suspend fun getCustomers(): CustomerCache?

    @Insert
    suspend fun insert(customers: CustomerCache)

    @Query("DELETE FROM CustomerCache")
    suspend fun deleteCustomers()
}