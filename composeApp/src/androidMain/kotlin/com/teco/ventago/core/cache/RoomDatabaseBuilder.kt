package com.teco.ventago.core.cache

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.teco.ventago.core.cache.room.CacheDatabase

fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<CacheDatabase> {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath("cache_database.db")

    return Room.databaseBuilder<CacheDatabase>(
        context = appContext,
        name = dbFile.absolutePath,
    )
}