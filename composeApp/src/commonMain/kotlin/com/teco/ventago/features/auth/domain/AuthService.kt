package com.teco.ventago.features.auth.domain

import androidx.compose.ui.input.key.Key.Companion.Menu
import com.teco.ventago.Configs
import com.teco.ventago.core.SecureStorage
import com.teco.ventago.core.cache.ICacheService
import com.teco.ventago.core.changes.IChangesManager
import com.teco.ventago.features.auth.data.repository.IAuthRepository
import com.teco.ventago.features.auth.domain.model.User
import com.teco.ventago.features.auth.domain.model.firebase.FirebaseUserDM
import com.teco.ventago.features.auth.domain.model.requests.CreateUserRequest
import com.teco.ventago.features.auth.domain.model.requests.EmailLoginRequest
import com.teco.ventago.features.auth.domain.model.response.AuthResponse
import com.teco.ventago.features.business.domain.model.Business
import com.teco.ventago.features.product.domain.model.Products
import com.teco.ventago.features.user.data.repository.IUserRepository
import com.teco.ventago.utils.ApiError
import com.teco.ventago.utils.ApiResponse
import com.teco.ventago.utils.AuthException
import com.teco.ventago.utils.base64.base64Decoded
import com.teco.ventago.utils.isExpired
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class AuthService(
    private val store: SecureStorage,
    private val firebase: IFirebaseService,
    private val repository: IAuthRepository,
    private val userRepository: IUserRepository,
    private val cache: ICacheService,
    private val changesManager: IChangesManager,
    private val client: HttpClient
) : IAuthService {
    var user = MutableStateFlow<User?>(null)
    private var userChangesJob: Job? = null

    init {
        CoroutineScope(Dispatchers.IO+ SupervisorJob()).launch {
            cache.getCache(User::class)?.let {
                if (it.missingBusiness) {
                    user.tryEmit(null)
                    signOut()
                } else {
                    user.tryEmit(it)
                    user = MutableStateFlow(it)
                    val business = cache.getCache(Business::class)
                    val products = cache.getCache(Products::class)
                    if (business != null && products != null) {
                        val businessId = business.businessId
                        val productsId = products.id
                        changesManager.initialize(businessId, productsId, it.userId)
                        listenUserChanges()
                    }
                }
            } ?: run {
                user.tryEmit(null)
                signOut()
            }
        }

    }

    private suspend fun listenUserChanges() {
        userChangesJob?.cancel()
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        userChangesJob = scope.launch {
            changesManager.userListener().onEach {
                if (it != 1) {
                    try {
                        val res = userRepository.getUserData(getJwtToken() ?: "")
                        val userData = User.fromAuthResponse(res)
                        cache.saveCache(userData)
                        user.update {
                            userData
                        }
                    } catch (e: Exception) {
                        if (e.message?.contains("AUTH_001") == true) {
                            refreshToken(client)
                            try {
                                val res = userRepository.getUserData(getJwtToken() ?: "")
                                val userData = User.fromAuthResponse(res)
                                cache.saveCache(userData)
                                user.update {
                                    userData
                                }
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
        val jwt: String? = store.string(forKey = SecureConstants.JWT_TOKEN)
        return jwt?.let {
            return !isTokenExpired(it)
        } ?: false
    }

    override fun getJwtToken(refresh: Boolean): String? {
        return store.string(forKey = if (!refresh) SecureConstants.JWT_TOKEN else SecureConstants.REFRESH_JWT_TOKEN)
    }

    override suspend fun refreshToken(client: HttpClient) {
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
        val map : Map<String, JsonElement>?
        try {
            map = response.data?.let { Json.decodeFromJsonElement<Map<String, JsonElement>>(it) }
        } catch (_: Exception) {
            return
        }

        map?.let {
            val accessToken = it["access_token"]!!.jsonPrimitive.content
            val refreshToken = it["refresh_token"]!!.jsonPrimitive.content
            saveJwt(accessToken)
            saveJwt(refreshToken, true)
        } ?: throw Exception("No data in response")
    }

    override suspend fun googleLogin(googleToken: String): AuthResponse {
        try {
            val res = repository.googleLogin(googleToken)
            if (res.providerToken.isBlank()) {
                throw AuthException(ApiError.F_AUTH_007)
            }
            val firebaseResponse = firebase.signInWithCustomToken(res.providerToken)
            if (firebaseResponse.success) {
                saveJwt(res.accessToken)
                saveJwt(res.refreshToken, true)
                val userData = User.fromAuthResponse(res)
                cache.saveCache(userData)

                user.update {
                    userData
                }
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

    override suspend fun emailLogin(request: EmailLoginRequest): AuthResponse {
        try {
            val res = repository.emailLogin(request)
            if (res.providerToken.isBlank()) {
                throw AuthException(ApiError.F_AUTH_007)
            }
            val firebaseResponse = firebase.signInWithCustomToken(res.providerToken)
            if (firebaseResponse.success) {
                saveJwt(res.accessToken)
                saveJwt(res.refreshToken, true)
                val userData = User.fromAuthResponse(res)
                cache.saveCache(userData)
                user.update {
                    userData
                }
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

    override suspend fun emailRegister(request: CreateUserRequest): AuthResponse {
        try {
            val res = repository.emailRegister(request)
            if (res.providerToken.isBlank()) {
                throw AuthException(ApiError.F_AUTH_007)
            }
            val firebaseResponse = firebase.signInWithCustomToken(res.providerToken)
            if (firebaseResponse.success) {
                saveJwt(res.accessToken)
                saveJwt(res.refreshToken, true)
                val userData = User.fromAuthResponse(res)
                cache.saveCache(userData)

                user.update {
                    userData
                }
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

    override suspend fun signOut() {
        try {
            // Cancel user changes listener job
            userChangesJob?.cancel()
            userChangesJob = null
            // Remove Firebase Realtime Database listeners
            changesManager.removeListeners()
            // Clear cache
            cache.clearAllCache()
            // Sign out from Firebase
            firebase.signOut()
            // Clear user state
            if (user.value != null) {
                user.update { null }
            }
            // Delete JWT tokens
            store.deleteObject(SecureConstants.JWT_TOKEN)
            store.deleteObject(SecureConstants.REFRESH_JWT_TOKEN)
        } catch (_: Exception) {
        }
    }

    override suspend fun businessRegistered() {
        val userModel = user.value
        userModel?.let { newUser ->
            cache.saveCache(newUser.copy(missingBusiness = false))
            user.update {
                newUser.copy(missingBusiness = false)
            }
        }
    }


    override fun setPremium(premium: Boolean) {
        val userModel = user.value
        userModel?.let { newUser ->
            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                userRepository.setPremium(premium)
                cache.saveCache(newUser.copy(premium = premium))
                withContext(Dispatchers.Main) {
                    user.update {
                        newUser.copy(premium = premium)
                    }
                }
            }
        }
    }

    override suspend fun deleteAccount(token: String): Boolean {
        val res = userRepository.deleteAccount(token)
        if (res) {
            firebase.deleteAccount()
            changesManager.removeListeners()
            cache.clearAllCache()
            user.update {
                null
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
        val aux = jwt.split(".")
        if (aux.isEmpty()) {
            return false
        }
        val data = Json.parseToJsonElement(aux[1].base64Decoded)
        val iat = data.jsonObject["iat"]
        return !isExpired(iat.toString())
    }

    private fun saveJwt(token: String, refresh: Boolean = false) {
        store.deleteObject(if (!refresh) SecureConstants.JWT_TOKEN else SecureConstants.REFRESH_JWT_TOKEN)
        store.set(
            if (!refresh) SecureConstants.JWT_TOKEN else SecureConstants.REFRESH_JWT_TOKEN,
            token
        )
    }
}

object SecureConstants {
    const val JWT_TOKEN = "JWT_TOKEN"
    const val REFRESH_JWT_TOKEN = "REFRESH_JWT_TOKEN"
}