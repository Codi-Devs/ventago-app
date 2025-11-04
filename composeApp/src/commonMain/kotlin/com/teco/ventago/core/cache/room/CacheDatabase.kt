package com.teco.ventago.core.cache.room

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.teco.ventago.core.cache.room.models.BranchesCache
import com.teco.ventago.core.cache.room.models.BusinessCache
import com.teco.ventago.core.cache.room.models.CustomerCache
import com.teco.ventago.core.cache.room.models.FinancialProfileCache
import com.teco.ventago.core.cache.room.models.ProductsCache
import com.teco.ventago.core.cache.room.models.UserCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@TypeConverters(value = [RoomTypeConverters::class])
@Database(entities = [
    UserCache::class,
    ProductsCache::class,
    CustomerCache::class,
    BranchesCache::class,
    FinancialProfileCache::class,
    BusinessCache::class], version = 5)
@ConstructedBy(CacheDatabaseConstructor::class)
abstract class CacheDatabase: RoomDatabase() {
    abstract fun getUserCacheDao(): UserCacheDao
    abstract fun getBusinessCacheDao(): BusinessCacheDao
    abstract fun getProductsCacheDao(): ProductsCacheDao
    abstract fun getCustomerCacheDao(): CustomersCacheDao
    abstract fun getBranchesCacheDao(): BranchesCacheDao
    abstract fun getFinancialProfileCacheDao(): FinancialProfileCacheDao
}

// Room compiler generates the `actual` implementations
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object CacheDatabaseConstructor : RoomDatabaseConstructor<CacheDatabase> {
    override fun initialize(): CacheDatabase
}

fun getCacheDatabase(builder: RoomDatabase.Builder<CacheDatabase>): CacheDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .fallbackToDestructiveMigration(true)
        .build()
}