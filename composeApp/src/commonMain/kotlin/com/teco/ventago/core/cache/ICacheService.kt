package com.teco.ventago.core.cache

import kotlin.reflect.KClass

interface ICacheService {
    suspend fun <T: Any> getCache(klass: KClass<T>): T?
    suspend fun <T: Any> getCache(key:String) : T?
    suspend fun <T> saveCache(data: T)
    suspend fun <T> saveCache(key:String, data: T)
    suspend fun clearCache(id: String)
    suspend fun clearAllCache()
}