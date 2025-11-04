package com.teco.ventago.core.cache.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.teco.ventago.core.cache.room.models.ProductsCache


@Dao
interface ProductsCacheDao {
    @Query("SELECT * FROM ProductsCache limit 1")
    suspend fun getProducts(): ProductsCache?

    @Insert
    suspend fun insert(user: ProductsCache)

    @Query("DELETE FROM ProductsCache")
    suspend fun deleteProducts()
}