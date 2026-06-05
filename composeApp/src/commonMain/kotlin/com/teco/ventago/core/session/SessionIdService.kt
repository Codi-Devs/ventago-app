package com.teco.ventago.core.session

import com.teco.ventago.Configs
import com.teco.ventago.core.SecureStorage
import com.teco.ventago.utils.randomUUID
import io.ktor.http.Url
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.days

interface ISessionIdService {
    suspend fun sessionIdForBackendRequest(): String
    suspend fun startSession(forceNew: Boolean = true): String
    fun clearSession()
}

interface SessionIdStore {
    fun string(key: String): String?
    fun long(key: String): Long?
    fun set(key: String, value: String): Boolean
    fun set(key: String, value: Long): Boolean
    fun delete(key: String): Boolean
}

class SecureStorageSessionIdStore(
    private val storage: SecureStorage
) : SessionIdStore {
    override fun string(key: String): String? = storage.string(key)
    override fun long(key: String): Long? = storage.long(key)
    override fun set(key: String, value: String): Boolean = storage.set(key, value)
    override fun set(key: String, value: Long): Boolean = storage.set(key, value)
    override fun delete(key: String): Boolean = storage.deleteObject(key)
}

class SessionIdService(
    private val store: SessionIdStore,
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val idGenerator: () -> String = { randomUUID() }
) : ISessionIdService {
    private val mutex = Mutex()

    override suspend fun sessionIdForBackendRequest(): String = mutex.withLock {
        val now = nowMillis()
        val currentId = store.string(SessionIdConstants.SESSION_ID_KEY)
        val expiresAt = store.long(SessionIdConstants.SESSION_ID_EXPIRES_AT_KEY) ?: 0L
        if (currentId != null && currentId.isValidSessionId() && expiresAt > now) {
            persist(currentId, now)
            currentId
        } else {
            createAndPersist(now)
        }
    }

    override suspend fun startSession(forceNew: Boolean): String = mutex.withLock {
        val now = nowMillis()
        if (!forceNew) {
            val currentId = store.string(SessionIdConstants.SESSION_ID_KEY)
            val expiresAt = store.long(SessionIdConstants.SESSION_ID_EXPIRES_AT_KEY) ?: 0L
            if (currentId != null && currentId.isValidSessionId() && expiresAt > now) {
                persist(currentId, now)
                return@withLock currentId
            }
        }
        createAndPersist(now)
    }

    override fun clearSession() {
        store.delete(SessionIdConstants.SESSION_ID_KEY)
        store.delete(SessionIdConstants.SESSION_ID_EXPIRES_AT_KEY)
    }

    private fun createAndPersist(now: Long): String {
        val sessionId = generateSessionId()
        persist(sessionId, now)
        return sessionId
    }

    private fun persist(sessionId: String, now: Long) {
        store.set(SessionIdConstants.SESSION_ID_KEY, sessionId)
        store.set(SessionIdConstants.SESSION_ID_EXPIRES_AT_KEY, now + SessionIdConstants.TTL.inWholeMilliseconds)
    }

    private fun generateSessionId(): String {
        val builder = StringBuilder(SessionIdConstants.SESSION_ID_LENGTH)
        while (builder.length < SessionIdConstants.SESSION_ID_LENGTH) {
            builder.append(idGenerator().filter { it.isLetterOrDigit() })
        }
        return builder.toString().take(SessionIdConstants.SESSION_ID_LENGTH)
    }

    private fun String.isValidSessionId(): Boolean {
        return length == SessionIdConstants.SESSION_ID_LENGTH &&
            all { it.isLetterOrDigit() }
    }
}

object SessionIdBackendUrlMatcher {
    fun isVentaGoBackendUrl(url: String): Boolean {
        return runCatching {
            val requestUrl = Url(url)
            requestUrl.sameOriginAs(Url(Configs.serverBasePath)) ||
                requestUrl.sameOriginAs(Url(Configs.ordersBasePath))
        }.getOrDefault(false)
    }

    private fun Url.sameOriginAs(other: Url): Boolean {
        return protocol == other.protocol && host == other.host && port == other.port
    }
}

object SessionIdConstants {
    const val HEADER_NAME = "X-SESSION-ID"
    const val SESSION_ID_KEY = "SESSION_ID"
    const val SESSION_ID_EXPIRES_AT_KEY = "SESSION_ID_EXPIRES_AT_MS"
    const val SESSION_ID_LENGTH = 32
    val TTL = 3.days
}
