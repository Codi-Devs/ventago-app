package com.teco.ventago.features.product.data.provider.category

import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.auth.domain.model.User
import com.teco.ventago.features.auth.domain.model.firebase.FirebaseUserDM
import com.teco.ventago.features.auth.domain.model.requests.CreateUserRequest
import com.teco.ventago.features.auth.domain.model.requests.EmailLoginRequest
import com.teco.ventago.features.auth.domain.model.response.AuthResponse
import com.teco.ventago.features.product.domain.model.Category
import com.teco.ventago.utils.ApiError
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CategoryProviderTest {
    @Test
    fun addCategorySendsBearerAndOmitsHmac() = runTest {
        val auth = FakeAuth()
        val captured = mutableListOf<Pair<String?, String?>>()
        val client = client(captured) {
            """{"status":true,"data":{"id_category":4}}"""
        }
        val response = CategoryProvider(client, auth).addCategory(sampleCategory(), menuId = 8)

        assertTrue(response.successful)
        assertEquals(1, captured.size)
        assertEquals("Bearer access-token", captured[0].first)
        assertNull(captured[0].second)
        assertEquals(0, auth.refreshes)
        client.close()
    }

    @Test
    fun addCategoryRefreshesOnceWhenErrorIsAnObject() = runTest {
        val auth = FakeAuth()
        val captured = mutableListOf<Pair<String?, String?>>()
        var calls = 0
        val client = client(captured) {
            calls += 1
            if (calls == 1) {
                """{"status":false,"error":{"code":"AUTH_001","message":"expired"}}"""
            } else {
                """{"status":true,"data":{"id_category":4}}"""
            }
        }
        val response = CategoryProvider(client, auth).addCategory(sampleCategory(), menuId = 8)

        assertTrue(response.successful)
        assertEquals(1, auth.refreshes)
        assertEquals(listOf("Bearer access-token", "Bearer refreshed-token"), captured.map { it.first })
        assertTrue(captured.all { it.second == null })
        client.close()
    }

    @Test
    fun addCategoryDoesNotRefreshAgainWhenTheRetryIsStillUnauthorized() = runTest {
        val auth = FakeAuth()
        val captured = mutableListOf<Pair<String?, String?>>()
        val client = client(captured) {
            """{"status":false,"error":"AUTH_001"}"""
        }
        val response = CategoryProvider(client, auth).addCategory(sampleCategory(), menuId = 8)

        assertFalse(response.successful)
        assertEquals(ApiError.AUTH_001, response.error)
        assertEquals(1, auth.refreshes)
        assertEquals(2, captured.size)
        client.close()
    }

    private fun sampleCategory() = Category(
        id = -1,
        name = "Bebidas",
        desc = "",
        active = false,
        items = emptyList(),
        order = 1,
    )

    private fun client(
        captured: MutableList<Pair<String?, String?>>,
        body: () -> String,
    ): HttpClient {
        val engine = MockEngine { request ->
            captured += request.headers[HttpHeaders.Authorization] to request.headers["http-x-api-token"]
            respond(
                content = body(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        return HttpClient(engine) {
            install(ContentNegotiation) { json() }
        }
    }

    private class FakeAuth : IAuthService {
        var refreshes = 0
        private var token = "access-token"

        override suspend fun refreshToken(client: HttpClient, failedAccessToken: String?) {
            refreshes += 1
            token = "refreshed-token"
        }

        override fun getJwtToken(refresh: Boolean): String = token
        override suspend fun googleLogin(googleToken: String, businessId: Int?): AuthResponse = error("unused")
        override suspend fun emailLogin(request: EmailLoginRequest): AuthResponse = error("unused")
        override suspend fun emailRegister(request: CreateUserRequest): AuthResponse = error("unused")
        override suspend fun sendPasswordResetEmail(email: String) = Unit
        override suspend fun businessRegistered() = Unit
        override suspend fun signOut() = Unit
        override fun getFirebaseUser(): Flow<FirebaseUserDM?> = emptyFlow()
        override fun getUser(): Flow<User?> = emptyFlow()
        override fun getUserSync(): User? = null
        override fun isAuthenticated(): Boolean = true
        override fun setPremium(premium: Boolean) = Unit
        override suspend fun deleteAccount(token: String): Boolean = false
    }
}
