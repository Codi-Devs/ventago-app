package com.teco.ventago.features.auth.domain

import com.teco.ventago.core.SecureStorage

/** Storage boundary for session tokens; platform failures are isolated during logout. */
interface AuthTokenStore {
    fun string(forKey: String): String?
    fun set(key: String, value: String): Boolean
    fun deleteObject(forKey: String): Boolean
}

class SecureStorageAuthTokenStore(private val storage: SecureStorage) : AuthTokenStore {
    override fun string(forKey: String): String? = storage.string(forKey)
    override fun set(key: String, value: String): Boolean = storage.set(key, value)
    override fun deleteObject(forKey: String): Boolean = storage.deleteObject(forKey)
}
