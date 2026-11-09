package com.teco.ventago.features.auth.domain

import com.teco.ventago.Configs
import com.teco.ventago.core.authz.AuthzJwtDecoder
import com.teco.ventago.core.cache.ICacheService
import com.teco.ventago.core.changes.IChangesManager
import com.teco.ventago.core.session.ISessionIdService
import com.teco.ventago.features.auth.data.repository.IAuthRepository
import com.teco.ventago.features.auth.domain.model.User
import com.teco.ventago.features.auth.domain.model.firebase.FirebaseUserDM
import com.teco.ventago.features.auth.domain.model.requests.CreateUserRequest
import com.teco.ventago.features.auth.domain.model.requests.EmailLoginRequest
import com.teco.ventago.features.auth.domain.model.response.AuthResponse
import com.teco.ventago.features.auth.domain.model.response.BusinessIds
import com.teco.ventago.features.business.domain.model.Business
import com.teco.ventago.features.pos.provisioning.domain.PosDeviceProvisioningService
import com.teco.ventago.features.pos.provisioning.domain.PosProvisioningException
import com.teco.ventago.features.pos.provisioning.domain.shouldAbortPosLogin
import com.teco.ventago.features.product.domain.model.Products
import com.teco.ventago.features.user.data.repository.IUserRepository
import com.teco.ventago.utils.ApiError
import com.teco.ventago.utils.ApiResponse
import com.teco.ventago.utils.AuthException
import com.teco.ventago.utils.BusinessSelectionRequiredException
import com.teco.ventago.utils.MustChangePasswordException
import com.teco.ventago.utils.isError
import com.teco.ventago.utils.base64.base64Decoded
import com.teco.ventago.utils.base64.base64UrlDecoded
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.datetime.Clock

private const val LAST_BUSINESS_KEY = "ventago.lastBusinessId"

class AuthService(
    private val store: AuthTokenStore,
    private val firebase: IFirebaseService,
    private val repository: IAuthRepository,
    private val userRepository: IUserRepository,
    private val cache: ICacheService,
    private val changesManager: IChangesManager,
    private val client: HttpClient,
    private val sessionIdService: ISessionIdService,
    private val posProvisioningService: PosDeviceProvisioningService,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
) : IAuthService {
    val user = MutableStateFlow<User?>(null)
    private var pendingUser: User? = null
    private var pendingProviderToken: String = ""
    private var stagedBusinessId: Int? = null
    private val tenantEpoch = MutableStateFlow(0)

    private enum class LoginBusinessSource { Bound, Explicit, Staged, Remembered, None }

    private data class LoginBusinessChoice(val id: Int?, val source: LoginBusinessSource)
    private var userChangesJob: Job? = null
    private val refreshSingleFlight = RefreshTokenSingleFlight()
    private val _sessionResolved = MutableStateFlow(false)

    init {
        scope.launch {
            try {
                cache.getCache(User::class)?.let {
                    val cachedUser = enrichUserWithCurrentToken(it)
                    if (cachedUser.missingBusiness) {
                        user.tryEmit(null)
                        signOut()
                    } else {
                        try {
                            val accessToken = getValidAccessTokenForCachedSession()
                            posProvisioningService.validateBusinessIds(
                                businessIds = cachedUser.businessIds.map { it.businessId },
                                accessToken = accessToken
                            )
                        } catch (e: PosProvisioningException) {
                            if (e.shouldAbortPosLogin()) {
                                user.tryEmit(null)
                                signOut()
                                return@launch
                            }
                        }
                        user.tryEmit(cachedUser)
                        val business = cache.getCache(Business::class)
                        val products = cache.getCache(Products::class)
                        if (business != null && products != null) {
                            val businessId = business.businessId
                            val productsId = products.id
                            changesManager.initialize(businessId, productsId, cachedUser.userId)
                            listenUserChanges()
                        }
                    }
                } ?: run {
                    user.tryEmit(null)
                    signOut()
                }
            } catch (_: SessionExpiredException) {
                // Refresh already cleared the session; resolve startup as a guest.
            } finally {
                _sessionResolved.value = true
            }
        }

    }

    override fun sessionResolved(): Flow<Boolean> = _sessionResolved.asStateFlow()

    private suspend fun getValidAccessTokenForCachedSession(): String? {
        val accessToken = getJwtToken()
        if (!accessToken.isNullOrBlank() && !isTokenExpired(accessToken)) {
            return accessToken
        }
        refreshToken(client, accessToken)
        return getJwtToken()
    }

    private suspend fun listenUserChanges() {
        userChangesJob?.cancel()
        userChangesJob = scope.launch {
            changesManager.userListener().onEach {
                if (it != 1) {
                    try {
                        val res = userRepository.getUserData(getJwtToken() ?: "")
                        val userData = enrichUserWithCurrentToken(User.fromAuthResponse(res))
                        cache.saveCache(userData)
                        user.update {
                            userData
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        if (e.message?.contains("AUTH_001") == true) {
                            try {
                                refreshToken(client)
                                val res = userRepository.getUserData(getJwtToken() ?: "")
                                val userData = enrichUserWithCurrentToken(User.fromAuthResponse(res))
                                cache.saveCache(userData)
                                user.update {
                                    userData
                                }
                            } catch (_: SessionExpiredException) {
                                return@onEach
                            } catch (ex: CancellationException) {
                                throw ex
                            } catch (ex: Exception) {
                                println(e)
                            }
                        }
                        println(e)
                    }
                }
            }.launchIn(this)
        }
    }

    override fun isAuthenticated(): Boolean {
        val userSync = getUserSync()
        if (userSync != null) {
            return true
        }
        val refreshToken = getJwtToken(true)
        return !refreshToken.isNullOrBlank() && !isTokenExpired(refreshToken)
    }

    override fun getJwtToken(refresh: Boolean): String? {
        return store.string(forKey = if (!refresh) SecureConstants.JWT_TOKEN else SecureConstants.REFRESH_JWT_TOKEN)
    }

    override suspend fun refreshToken(client: HttpClient, failedAccessToken: String?) {
        val tokenAtFailure = failedAccessToken ?: getJwtToken()
        refreshSingleFlight.refresh(
            failedAccessToken = tokenAtFailure,
            currentAccessToken = { getJwtToken() },
            refreshTokenAvailable = { !getJwtToken(refresh = true).isNullOrBlank() },
            performRefresh = { performRefreshToken(client) },
            onRefreshFailure = { signOut() },
        )
    }

    private suspend fun performRefreshToken(client: HttpClient) {
        val refresh = getJwtToken(true)
        val res = client.get(Configs.serverBasePath + "site/refresh-token") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.ContentType, "application/json")
                append(HttpHeaders.Authorization, "Bearer $refresh")
            }
            contentType(ContentType.Application.Json)
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        val map = try {
            response.data?.let { Json.decodeFromJsonElement<Map<String, JsonElement>>(it) }
        } catch (e: Exception) {
            throw IllegalStateException("Invalid refresh token response", e)
        } ?: throw IllegalStateException("No data in refresh token response")

        val accessToken = map["access_token"]?.jsonPrimitive?.contentOrNull
        val refreshToken = map["refresh_token"]?.jsonPrimitive?.contentOrNull
        if (!accessToken.isNullOrBlank() && !refreshToken.isNullOrBlank()) {
            saveJwt(accessToken)
            saveJwt(refreshToken, true)
            updateCurrentUserAuthz(accessToken)
        } else {
            throw IllegalStateException("Refresh token response is missing tokens")
        }
    }

    override fun tenantEpoch(): Flow<Int> = tenantEpoch.asStateFlow()

    override suspend fun googleLogin(googleToken: String, businessId: Int?): AuthResponse {
        val choice = resolveLoginBusinessId(businessId)
        return try {
            finishGoogleLogin(googleToken, choice.id)
        } catch (e: BusinessSelectionRequiredException) {
            throw e
        } catch (e: AuthException) {
            if (shouldRetryWithoutRememberedBusiness(e, choice.source)) {
                store.deleteObject(LAST_BUSINESS_KEY)
                try {
                    finishGoogleLogin(googleToken, null)
                } catch (retry: BusinessSelectionRequiredException) {
                    throw retry
                } catch (retry: Exception) {
                    failLogin(retry)
                }
            } else {
                failLogin(e)
            }
        } catch (e: Exception) {
            failLogin(e)
        }
    }

    override fun stageBusinessSelection(businessId: Int) {
        stagedBusinessId = businessId
    }

    override suspend fun emailLogin(request: EmailLoginRequest): AuthResponse {
        val choice = resolveLoginBusinessId(request.businessId)
        return try {
            finishEmailLogin(request.copy(businessId = choice.id))
        } catch (e: BusinessSelectionRequiredException) {
            throw e
        } catch (e: AuthException) {
            if (shouldRetryWithoutRememberedBusiness(e, choice.source)) {
                store.deleteObject(LAST_BUSINESS_KEY)
                try {
                    finishEmailLogin(request.copy(businessId = null))
                } catch (retry: BusinessSelectionRequiredException) {
                    throw retry
                } catch (retry: Exception) {
                    failLogin(retry)
                }
            } else {
                failLogin(e)
            }
        } catch (e: Exception) {
            failLogin(e)
        }
    }

    override suspend fun switchBusiness(businessId: Int) {
        if (businessId <= 0) throw AuthException(ApiError.AUTH_001)
        if (posProvisioningService.isRequired()) throw PosProvisioningException.BusinessMismatch
        withContext(NonCancellable) {
            val access = getJwtToken() ?: throw AuthException(ApiError.AUTH_001)
            val res = client.post(Configs.serverBasePath + "auth/switch-business") {
                headers {
                    append(HttpHeaders.Accept, "*/*")
                    append(HttpHeaders.Authorization, "Bearer $access")
                }
                contentType(ContentType.Application.Json)
                setBody("""{"business_id":$businessId}""")
            }
            val response = ApiResponse.fromJson(res.body())
            if (response.error.isError()) {
                throw mapPosAuthFailure(AuthException(response.error ?: ApiError.AUTH_001))
            }
            val map = response.data?.let { Json.decodeFromJsonElement<Map<String, JsonElement>>(it) }
                ?: throw IllegalStateException("No data in business switch response")
            val accessToken = map["access_token"]?.jsonPrimitive?.contentOrNull
            val refreshToken = map["refresh_token"]?.jsonPrimitive?.contentOrNull
            if (accessToken.isNullOrBlank() || refreshToken.isNullOrBlank()) {
                throw IllegalStateException("Business switch response is missing tokens")
            }
            val current = user.value ?: pendingUser
            val ids = current?.businessIds.orEmpty()
            val selected = ids.filter { it.businessId == businessId }
            val rest = ids.filter { it.businessId != businessId }
            val ordered = if (selected.isEmpty()) {
                listOf(BusinessIds(businessId, -1)) + rest
            } else {
                selected + rest
            }
            val providerToken = map["token"]?.jsonPrimitive?.contentOrNull
            if (providerToken.isNullOrBlank()) throw AuthException(ApiError.F_AUTH_007)
            val signedIn = firebase.signInWithCustomToken(providerToken)
            if (!signedIn.success) throw AuthException(ApiError.F_AUTH_007)
            pendingUser = null
            pendingProviderToken = ""
            changesManager.removeListeners()
            runCatching { cache.clearAllCache() }
            user.update { null }
            yield()
            saveJwt(accessToken)
            saveJwt(refreshToken, true)
            store.set(LAST_BUSINESS_KEY, businessId.toString())
            if (current != null) {
                val next = enrichUserWithCurrentToken(current.copy(businessIds = ordered), accessToken)
                cache.saveCache(next)
                user.update { next }
                tenantEpoch.update { it + 1 }
                listenUserChanges()
            }
        }
    }

    override suspend fun emailRegister(request: CreateUserRequest): AuthResponse {
        try {
            sessionIdService.startSession(forceNew = true)
            val res = repository.emailRegister(request)
            if (res.providerToken.isBlank()) {
                throw AuthException(ApiError.F_AUTH_007)
            }
            validateAccessTokenClaimsBeforeLogin(res.accessToken)
            val firebaseResponse = firebase.signInWithCustomToken(res.providerToken)
            if (firebaseResponse.success) {
                saveJwt(res.accessToken)
                saveJwt(res.refreshToken, true)
                val userData = enrichUserWithCurrentToken(User.fromAuthResponse(res), res.accessToken)
                cache.saveCache(userData)

                user.update {
                    userData
                }
                listenUserChanges()
            } else {
                throw AuthException(ApiError.F_AUTH_007)
            }
            return res
        } catch (e: Exception) {
            user.update {
                null
            }
            throw e
        }
    }

    override fun getFirebaseUser(): Flow<FirebaseUserDM?> = firebase.getUser()

    override fun getUser(): Flow<User?> = user

    override fun getUserSync(): User? = user.value

    override suspend fun signOut(): Unit = withContext(NonCancellable) {
        pendingUser = null
        pendingProviderToken = ""
        user.value = null
        // The listener can sign itself out. Finish cleanup even after cancelling its job,
        // and don't let a remote Firebase failure prevent local session removal.
        userChangesJob?.cancel()
        userChangesJob = null
        runCatching { changesManager.removeListeners() }
        runCatching { posProvisioningService.clear() }
        runCatching { cache.clearAllCache() }
        runCatching { firebase.signOut() }
        runCatching { store.deleteObject(SecureConstants.JWT_TOKEN) }
        runCatching { store.deleteObject(SecureConstants.REFRESH_JWT_TOKEN) }
        runCatching { sessionIdService.clearSession() }
        // Failure deduplication is reset only when a new refresh token is saved.
    }

    override suspend fun businessRegistered() {
        val userModel = user.value
        userModel?.let { newUser ->
            val updatedUser = enrichUserWithCurrentToken(newUser.copy(missingBusiness = false))
            cache.saveCache(updatedUser)
            user.update {
                updatedUser
            }
        }
    }


    override fun setPremium(premium: Boolean) {
        val userModel = user.value
        userModel?.let { newUser ->
            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                userRepository.setPremium(premium)
                val updatedUser = enrichUserWithCurrentToken(newUser.copy(premium = premium))
                cache.saveCache(updatedUser)
                withContext(Dispatchers.Main) {
                    user.update {
                        updatedUser
                    }
                }
            }
        }
    }

    override suspend fun deleteAccount(token: String): Boolean {
        val res = userRepository.deleteAccount(token)
        if (res) {
            try {
                userChangesJob?.cancel()
                userChangesJob = null
                changesManager.removeListeners()
                try {
                    firebase.signOut()
                } catch (_: Exception) {
                }
                cache.clearAllCache()
                user.update {
                    null
                }
                store.deleteObject(SecureConstants.JWT_TOKEN)
                store.deleteObject(SecureConstants.REFRESH_JWT_TOKEN)
            } finally {
                sessionIdService.clearSession()
            }
        }
        return res
    }

    override suspend fun sendPasswordResetEmail(email: String) {
        try {
            firebase.sendPasswordResetEmail(email)
        } catch (ignored: Exception) {
        }
    }

    private fun isTokenExpired(jwt: String): Boolean {
        val parts = jwt.split(".")
        if (parts.size < 2) {
            return true
        }
        val payload = runCatching { parts[1].base64UrlDecoded }
            .getOrElse { runCatching { parts[1].base64Decoded }.getOrNull() }
            ?: return true
        val data = runCatching { Json.parseToJsonElement(payload) }.getOrNull() ?: return true
        val exp = data.jsonObject["exp"]?.jsonPrimitive?.longOrNull ?: return true
        return exp <= Clock.System.now().epochSeconds
    }

    private suspend fun finishGoogleLogin(googleToken: String, businessId: Int?): AuthResponse {
        pendingUser = null
        pendingProviderToken = ""
        sessionIdService.startSession(forceNew = true)
        val res = repository.googleLogin(googleToken, businessId)
        return storeLogin(res) { posProvisioningService.validateLogin(res) }
    }

    private suspend fun finishEmailLogin(request: EmailLoginRequest): AuthResponse {
        pendingUser = null
        pendingProviderToken = ""
        sessionIdService.startSession(forceNew = true)
        val res = repository.emailLogin(request)
        return storeLogin(res) { posProvisioningService.validateLogin(res) }
    }

    private suspend fun storeLogin(res: AuthResponse, validate: suspend () -> Unit): AuthResponse {
        if (res.providerToken.isBlank()) {
            throw AuthException(ApiError.F_AUTH_007)
        }
        validateAccessTokenClaimsBeforeLogin(res.accessToken)
        validate()
        rememberBusiness(res)
        val userData = enrichUserWithCurrentToken(User.fromAuthResponse(res), res.accessToken)
        saveJwt(res.accessToken)
        saveJwt(res.refreshToken, true)
        val firebaseResponse = firebase.signInWithCustomToken(res.providerToken)
        if (!firebaseResponse.success) {
            throw AuthException(ApiError.F_AUTH_007)
        }
        cache.saveCache(userData)
        user.update { userData }
        listenUserChanges()
        return res
    }

    override suspend fun confirmPendingLogin() {
        val pending = pendingUser ?: return
        val token = pendingProviderToken
        if (token.isBlank()) throw AuthException(ApiError.F_AUTH_007)
        val firebaseResponse = firebase.signInWithCustomToken(token)
        if (!firebaseResponse.success) throw AuthException(ApiError.F_AUTH_007)
        pendingUser = null
        pendingProviderToken = ""
        cache.saveCache(pending)
        user.update { pending }
        listenUserChanges()
    }

    private fun shouldRetryWithoutRememberedBusiness(error: AuthException, source: LoginBusinessSource): Boolean {
        return error.error == ApiError.AUTH_004 && source == LoginBusinessSource.Remembered && !posProvisioningService.isRequired()
    }

    private suspend fun failLogin(error: Exception): Nothing {
        user.update { null }
        if (error.shouldAbortPosLogin()) {
            signOut()
        }
        throw mapPosAuthFailure(error)
    }

    private suspend fun resolveLoginBusinessId(explicit: Int?): LoginBusinessChoice {
        val bound = posProvisioningService.boundBusinessIdOrNull()
        if (bound != null && bound > 0) {
            stagedBusinessId = null
            return LoginBusinessChoice(bound, LoginBusinessSource.Bound)
        }
        if (explicit != null && explicit > 0) {
            return LoginBusinessChoice(explicit, LoginBusinessSource.Explicit)
        }
        stagedBusinessId = null
        return LoginBusinessChoice(null, LoginBusinessSource.None)
    }

    private fun rememberBusiness(response: AuthResponse) {
        val id = response.activeBusinessId.takeIf { it > 0 }
            ?: response.businesses.firstOrNull()?.businessId?.takeIf { it > 0 }
            ?: return
        store.set(LAST_BUSINESS_KEY, id.toString())
    }

    private fun mapPosAuthFailure(error: Exception): Exception {
        if (error is AuthException && error.error == ApiError.AUTH_004 && posProvisioningService.isRequired()) {
            return PosProvisioningException.BusinessMismatch
        }
        return error
    }

    private fun saveJwt(token: String, refresh: Boolean = false) {
        store.deleteObject(if (!refresh) SecureConstants.JWT_TOKEN else SecureConstants.REFRESH_JWT_TOKEN)
        store.set(
            if (!refresh) SecureConstants.JWT_TOKEN else SecureConstants.REFRESH_JWT_TOKEN,
            token
        )
        if (refresh) {
            refreshSingleFlight.resetFailureState()
        }
    }

    private fun enrichUserWithCurrentToken(userModel: User, accessToken: String? = getJwtToken()): User {
        val claims = AuthzJwtDecoder.decode(accessToken) ?: return userModel
        return userModel.copy(
            scopes = claims.scopes,
            isSubUser = claims.isSubUser,
            isOwnerMain = claims.isOwnerMain
        )
    }

    private fun updateCurrentUserAuthz(accessToken: String) {
        val currentUser = user.value ?: return
        val enrichedUser = enrichUserWithCurrentToken(currentUser, accessToken)
        if (enrichedUser == currentUser) return
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            cache.saveCache(enrichedUser)
        }
        user.update { enrichedUser }
    }

    private suspend fun validateAccessTokenClaimsBeforeLogin(accessToken: String) {
        val claims = AuthzJwtDecoder.decode(accessToken) ?: return
        if (claims.mustChangePassword) {
            signOut()
            throw MustChangePasswordException()
        }
    }
}

object SecureConstants {
    const val JWT_TOKEN = "JWT_TOKEN"
    const val REFRESH_JWT_TOKEN = "REFRESH_JWT_TOKEN"
}
