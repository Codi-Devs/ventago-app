package com.teco.ventago.core.cache.room

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.teco.ventago.core.cache.room.models.UserCache
import com.teco.ventago.features.auth.domain.model.User
import kotlinx.coroutines.flow.Flow


@Dao
interface UserCacheDao {
    @Query("SELECT * FROM UserCache limit 1")
    suspend fun getUser(): UserCache?

    @Insert
    suspend fun insert(user: UserCache)

    @Query("DELETE FROM UserCache")
    suspend fun deleteUser()
}


