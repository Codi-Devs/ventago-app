package com.teco.ventago.core.session

import com.teco.ventago.Configs
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionIdServiceTest {
    @Test
    fun generatedSessionIdIsAlphanumericWithoutHyphens() = runTest {
        val store = FakeSessionIdStore()
        val service = SessionIdService(
            store = store,
            nowMillis = { 1_000L },
            idGenerator = { "12345678-90ab-cdef-1234-567890abcdef" }
        )

        val sessionId = service.sessionIdForBackendRequest()

        assertEquals(SessionIdConstants.SESSION_ID_LENGTH, sessionId.length)
        assertTrue(sessionId.all { it.isLetterOrDigit() })
        assertFalse(sessionId.contains("-"))
        assertEquals("1234567890abcdef1234567890abcdef", sessionId)
    }

    @Test
    fun validSessionIdIsReusedAndTtlIsRefreshed() = runTest {
        var now = 5_000L
        val store = FakeSessionIdStore().apply {
            set(SessionIdConstants.SESSION_ID_KEY, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
            set(SessionIdConstants.SESSION_ID_EXPIRES_AT_KEY, now + 1_000L)
        }
        val service = SessionIdService(
            store = store,
            nowMillis = { now },
            idGenerator = { "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb" }
        )

        val sessionId = service.sessionIdForBackendRequest()

        assertEquals("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", sessionId)
        assertEquals(
            now + SessionIdConstants.TTL.inWholeMilliseconds,
            store.long(SessionIdConstants.SESSION_ID_EXPIRES_AT_KEY)
        )
    }

    @Test
    fun expiredSessionIdIsReplaced() = runTest {
        val now = 10_000L
        val store = FakeSessionIdStore().apply {
            set(SessionIdConstants.SESSION_ID_KEY, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
            set(SessionIdConstants.SESSION_ID_EXPIRES_AT_KEY, now)
        }
        val service = SessionIdService(
            store = store,
            nowMillis = { now },
            idGenerator = { "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb" }
        )

        val sessionId = service.sessionIdForBackendRequest()

        assertEquals("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", sessionId)
        assertEquals(sessionId, store.string(SessionIdConstants.SESSION_ID_KEY))
    }

    @Test
    fun startSessionForceNewRotatesSessionId() = runTest {
        val ids = mutableListOf(
            "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
            "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"
        )
        val store = FakeSessionIdStore()
        val service = SessionIdService(
            store = store,
            nowMillis = { 1_000L },
            idGenerator = { ids.removeAt(0) }
        )

        val first = service.startSession(forceNew = true)
        val second = service.startSession(forceNew = true)

        assertNotEquals(first, second)
        assertEquals("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", second)
        assertEquals(second, store.string(SessionIdConstants.SESSION_ID_KEY))
    }

    @Test
    fun startSessionCanReuseExistingSessionWhenForceNewIsFalse() = runTest {
        val store = FakeSessionIdStore().apply {
            set(SessionIdConstants.SESSION_ID_KEY, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
            set(SessionIdConstants.SESSION_ID_EXPIRES_AT_KEY, 10_000L)
        }
        val service = SessionIdService(
            store = store,
            nowMillis = { 1_000L },
            idGenerator = { "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb" }
        )

        val sessionId = service.startSession(forceNew = false)

        assertEquals("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", sessionId)
    }

    @Test
    fun clearSessionRemovesSessionIdAndExpiry() {
        val store = FakeSessionIdStore().apply {
            set(SessionIdConstants.SESSION_ID_KEY, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
            set(SessionIdConstants.SESSION_ID_EXPIRES_AT_KEY, 10_000L)
        }
        val service = SessionIdService(store = store)

        service.clearSession()

        assertNull(store.string(SessionIdConstants.SESSION_ID_KEY))
        assertNull(store.long(SessionIdConstants.SESSION_ID_EXPIRES_AT_KEY))
    }

    @Test
    fun backendUrlMatcherIncludesVentaGoBackendsOnly() {
        assertTrue(SessionIdBackendUrlMatcher.isVentaGoBackendUrl(Configs.serverBasePath + "auth/email-login"))
        assertTrue(SessionIdBackendUrlMatcher.isVentaGoBackendUrl(Configs.ordersBasePath + "/api/v1/orders/create"))
        assertFalse(SessionIdBackendUrlMatcher.isVentaGoBackendUrl("https://la.storage.bunnycdn.com/ventago/products/image.jpg"))
        assertFalse(SessionIdBackendUrlMatcher.isVentaGoBackendUrl(Configs.ordersBasePath + ".example.com/api/v1/orders/create"))
    }

    private class FakeSessionIdStore : SessionIdStore {
        private val values = mutableMapOf<String, Any>()

        override fun string(key: String): String? = values[key] as? String
        override fun long(key: String): Long? = values[key] as? Long
        override fun set(key: String, value: String): Boolean {
            values[key] = value
            return true
        }

        override fun set(key: String, value: Long): Boolean {
            values[key] = value
            return true
        }

        override fun delete(key: String): Boolean {
            values.remove(key)
            return true
        }
    }
}
