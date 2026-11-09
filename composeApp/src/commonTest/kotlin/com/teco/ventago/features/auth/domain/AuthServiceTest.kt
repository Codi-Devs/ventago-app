package com.teco.ventago.features.auth.domain

import com.teco.ventago.AppDistribution
import com.teco.ventago.core.cache.ICacheService
import com.teco.ventago.core.changes.IChangesManager
import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.session.ISessionIdService
import com.teco.ventago.features.auth.data.repository.IAuthRepository
import com.teco.ventago.features.auth.domain.model.User
import com.teco.ventago.features.auth.domain.model.firebase.CustomTokenResult
import com.teco.ventago.features.auth.domain.model.firebase.FirebaseUserDM
import com.teco.ventago.features.auth.domain.model.requests.CreateUserRequest
import com.teco.ventago.features.auth.domain.model.requests.EmailLoginRequest
import com.teco.ventago.features.auth.domain.model.response.AuthResponse
import com.teco.ventago.features.business.domain.model.Business
import com.teco.ventago.features.pos.provisioning.data.repository.IPosDeviceProvisioningRepository
import com.teco.ventago.features.pos.provisioning.domain.*
import com.teco.ventago.features.pos.provisioning.domain.model.PosAgentConfigResult
import com.teco.ventago.features.pos.provisioning.domain.model.PosAgentDeviceConfig
import com.teco.ventago.features.pos.provisioning.domain.model.PosDeviceConfig
import com.teco.ventago.features.product.domain.model.Products
import com.teco.ventago.features.user.data.repository.IUserRepository
import com.teco.ventago.utils.ApiError
import com.teco.ventago.utils.AuthException
import com.teco.ventago.utils.BusinessChoice
import com.teco.ventago.utils.BusinessSelectionRequiredException
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class AuthServiceTest {
    @Test
    fun signOutPublishesGuestAndAttemptsEveryCleanupEvenWhenAnOperationFails() = runTest {
        val operations = listOf("listeners", "cache", "firebase", SecureConstants.JWT_TOKEN,
            SecureConstants.REFRESH_JWT_TOKEN, "session")
        for (failures in operations.map { setOf(it) } + listOf(operations.toSet())) {
            val f = Fixture(coroutineContext, failures = failures)
            try {
                runCurrent()
                assertNotNull(f.auth.getUserSync())
                f.auth.signOut()
                assertNull(f.auth.getUserSync())
                assertTrue(operations.all { it in f.cleanupCalls })
                assertTrue(f.guestDuringCleanup.all { it })
                assertTrue(f.uncaught.isEmpty())
            } finally { f.close() }
        }
    }

    @Test
    fun startupWithCachedUserAndMissingRefreshResolvesAsGuestEvenIfCleanupFails() = runTest {
        val f = Fixture(coroutineContext, expiredAccess = true,
            failures = setOf("firebase", SecureConstants.JWT_TOKEN, "session"))
        try {
            runCurrent()
            assertTrue(f.auth.sessionResolved().first())
            assertNull(f.auth.getUserSync())
            assertEquals(1, f.cleanupCalls.count { it == "firebase" })
            assertTrue(f.uncaught.isEmpty())
        } finally { f.close() }
    }

    @Test
    fun userListenerExpiresSessionAndCompletesCleanupAfterCancellingItself() = runTest {
        val f = Fixture(coroutineContext)
        try {
            runCurrent()
            assertNotNull(f.auth.getUserSync())
            assertEquals(1, f.events.subscriptionCount.value)
            f.events.emit(2)
            advanceUntilIdle()
            assertNull(f.auth.getUserSync())
            assertEquals(1, f.userReads)
            assertEquals(1, f.cleanupCalls.count { it == "firebase" })
            assertTrue("session" in f.cleanupCalls)
            assertNull(f.tokens[SecureConstants.JWT_TOKEN])
            assertEquals(0, f.events.subscriptionCount.value)
            assertTrue(f.uncaught.isEmpty())
        } finally { f.close() }
    }

    @Test
    fun selectionRequiredDoesNotStoreSessionTokens() = runTest {
        val f = Fixture(coroutineContext, emailHandler = {
            throw BusinessSelectionRequiredException(listOf(BusinessChoice(3, "Norte", "owner")))
        })
        try {
            runCurrent()
            val accessBefore = f.tokens[SecureConstants.JWT_TOKEN]
            assertFailsWith<BusinessSelectionRequiredException> {
                f.auth.emailLogin(EmailLoginRequest("person@example.invalid", "secret"))
            }
            assertEquals(accessBefore, f.tokens[SecureConstants.JWT_TOKEN])
            assertNull(f.tokens[SecureConstants.REFRESH_JWT_TOKEN])
        } finally { f.close() }
    }

    @Test
    fun loginDoesNotSendRememberedBusinessId() = runTest {
        val calls = mutableListOf<Int?>()
        val f = Fixture(coroutineContext, emailHandler = { request ->
            calls += request.businessId
            throw BusinessSelectionRequiredException(listOf(BusinessChoice(4, "Sur", "staff")))
        })
        try {
            runCurrent()
            f.tokens["ventago.lastBusinessId"] = "99"
            assertFailsWith<BusinessSelectionRequiredException> {
                f.auth.emailLogin(EmailLoginRequest("person@example.invalid", "secret"))
            }
            assertEquals(listOf<Int?>(null), calls)
            assertEquals("99", f.tokens["ventago.lastBusinessId"])
        } finally { f.close() }
    }

    @Test
    fun posBoundBusinessIgnoresRememberedIdAndMapsAuth004ToMismatch() = runTest {
        val calls = mutableListOf<Int?>()
        val f = Fixture(
            coroutineContext,
            emailHandler = { request ->
                calls += request.businessId
                throw AuthException(ApiError.AUTH_004)
            },
            posBuild = true,
            boundBusinessId = 7,
        )
        try {
            runCurrent()
            f.tokens["ventago.lastBusinessId"] = "99"
            val error = assertFailsWith<PosProvisioningException> {
                f.auth.emailLogin(EmailLoginRequest("person@example.invalid", "secret"))
            }
            assertTrue(error is PosProvisioningException.BusinessMismatch)
            assertEquals(listOf<Int?>(7), calls)
            assertEquals("99", f.tokens["ventago.lastBusinessId"])
        } finally { f.close() }
    }

    @Test
    fun switchBusinessClearsTheUserBeforePublishingTheNextTenant() = runTest {
        val client = HttpClient(MockEngine {
            respond(
                """{"success":true,"data":{"access_token":"header.e30.sig","refresh_token":"r","token":"custom"}}""",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val f = Fixture(coroutineContext, httpClient = client)
        try {
            val seen = mutableListOf<Int?>()
            val job = launch { f.auth.getUser().collect { seen += it?.userId } }
            runCurrent()
            f.tokens[SecureConstants.REFRESH_JWT_TOKEN] = "refresh"
            f.auth.switchBusiness(4)
            runCurrent()
            job.cancel()
            assertTrue(seen.contains(null))
            assertEquals(1, f.auth.tenantEpoch().first())
            assertTrue(f.cleanupCalls.contains("cache"))
        } finally { f.close() }
    }

    private class Fixture(context: CoroutineContext, expiredAccess: Boolean = false,
        private val failures: Set<String> = emptySet(),
        private val emailHandler: suspend (EmailLoginRequest) -> AuthResponse = { error("Unused") },
        posBuild: Boolean = false,
        boundBusinessId: Int? = null,
        httpClient: HttpClient = HttpClient()) {
        val cleanupCalls = mutableListOf<String>()
        val guestDuringCleanup = mutableListOf<Boolean>()
        val uncaught = mutableListOf<Throwable>()
        private val client = httpClient
        val events = MutableSharedFlow<Int>()
        val tokens = mutableMapOf(SecureConstants.JWT_TOKEN to
            if (expiredAccess) "expired" else "header.eyJleHAiOjQxMDI0NDQ4MDB9.signature")
        var userReads = 0
        private val scope = CoroutineScope(context + SupervisorJob() +
            CoroutineExceptionHandler { _, error -> uncaught.add(error) })
        private val cachedUser = User("uid", "test@example.invalid", "Test", false, true,
            false, 1, emptyList())
        lateinit var auth: AuthService
            private set

        private fun cleanup(operation: String) {
            cleanupCalls.add(operation)
            guestDuringCleanup.add(auth.getUserSync() == null)
            if (operation in failures) error("Simulated cleanup failure")
        }

        init {
            val cache = object : ICacheService {
                @Suppress("UNCHECKED_CAST")
                override suspend fun <T : Any> getCache(klass: KClass<T>): T? = when (klass) {
                    User::class -> cachedUser
                    Business::class -> Business(JsonObject(emptyMap()))
                    Products::class -> Products(1, true, emptyList())
                    else -> null
                } as T?
                override suspend fun <T : Any> getCache(key: String): T? = null
                override suspend fun <T> saveCache(data: T) = Unit
                override suspend fun <T> saveCache(key: String, data: T) = Unit
                override suspend fun clearCache(id: String) = Unit
                override suspend fun clearAllCache() { cleanup("cache") }
            }
            val changes = object : IChangesManager {
                override fun productsListener() = emptyFlow<Int>()
                override fun businessListener() = emptyFlow<Int>()
                override fun financialListener() = emptyFlow<Int>()
                override fun customersListener() = emptyFlow<Int>()
                override fun branchesListener() = emptyFlow<Int>()
                override fun userListener(): Flow<Int> = events
                override fun purchaseListener() = emptyFlow<Int>()
                override fun addedBusinessListener() = emptyFlow<Int>()
                override suspend fun productsChanged() = Unit
                override suspend fun businessChanged() = Unit
                override suspend fun branchesChanged() = Unit
                override suspend fun financialChanged() = Unit
                override suspend fun customersChanged() = Unit
                override suspend fun userChanged() = Unit
                override fun removeListeners() { cleanup("listeners") }
                override fun initialize(businessId: Int, menuId: Int, userId: Int) = Unit
            }
            val firebase = object : IFirebaseService {
                override suspend fun signInWithCustomToken(token: String) = CustomTokenResult(true, "uid")
                override fun getUser(): Flow<FirebaseUserDM?> = flowOf(null)
                override suspend fun signOut() { yield(); cleanup("firebase") }
                override suspend fun sendPasswordResetEmail(email: String) = Unit
            }
            val repository = object : IAuthRepository {
                override suspend fun googleLogin(googleToken: String, businessId: Int?): AuthResponse = error("Unused")
                override suspend fun emailLogin(request: EmailLoginRequest): AuthResponse = emailHandler(request)
                override suspend fun emailRegister(request: CreateUserRequest): AuthResponse = error("Unused")
            }
            val userRepository = object : IUserRepository {
                override suspend fun setPremium(premium: Boolean) = false
                override suspend fun deleteAccount(token: String) = false
                override suspend fun getUserData(token: String): AuthResponse {
                    userReads++
                    error("AUTH_001")
                }
            }
            val pos = PosDeviceProvisioningService(AppDistribution(posBuild),
                object : IPosAgentConfigReader {
                    override suspend fun getDeviceConfig(): PosAgentConfigResult? = null
                }, object : IPosDeviceProvisioningRepository {
                    override suspend fun getPosConfig(deviceId: String, accessToken: String?): PosDeviceConfig = error("Unused")
                }, object : ILoggerService { override fun sendLog(log: Log) = Unit },
                InMemoryPosDeviceBindingStore(
                    boundBusinessId?.let { PosAgentDeviceConfig("device", it, "0000", "001") }
                ))
            auth = AuthService(object : AuthTokenStore {
                override fun string(forKey: String) = tokens[forKey]
                override fun set(key: String, value: String): Boolean { tokens[key] = value; return true }
                override fun deleteObject(forKey: String): Boolean {
                    cleanup(forKey)
                    tokens.remove(forKey)
                    return true
                }
            }, firebase, repository, userRepository, cache, changes, client,
                object : ISessionIdService {
                    override suspend fun sessionIdForBackendRequest() = "session"
                    override suspend fun startSession(forceNew: Boolean) = "session"
                    override fun clearSession() { cleanup("session") }
                }, pos, scope)
        }
        fun close() { scope.cancel(); client.close() }
    }
}
