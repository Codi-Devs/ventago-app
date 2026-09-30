package com.teco.ventago.features.auth.domain

import com.teco.ventago.features.auth.domain.model.User
import com.teco.ventago.features.auth.domain.model.firebase.FirebaseUserDM
import com.teco.ventago.features.auth.domain.model.requests.CreateUserRequest
import com.teco.ventago.features.auth.domain.model.requests.EmailLoginRequest
import com.teco.ventago.features.auth.domain.model.response.AuthResponse
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface IAuthService {
    /**
     * Renews tokens, or signs out and throws [SessionExpiredException] on refresh failure.
     * This domain cancellation stops authenticated work without a fatal root-launch error.
     * Catch it before CancellationException if handling it explicitly; normal cancellation
     * must still be rethrown. Clear caller loading state in finally. runCatching and
     * catch(Exception) also capture it. Navigation observes the cleared user independently.
     * Current policy also signs out on transport/server/response failures during refresh.
     */
    suspend fun refreshToken(client: HttpClient, failedAccessToken: String? = null)
    suspend fun googleLogin(googleToken: String): AuthResponse
    suspend fun emailLogin(request: EmailLoginRequest): AuthResponse
    suspend fun emailRegister(request: CreateUserRequest): AuthResponse
    suspend fun sendPasswordResetEmail(email: String)
    suspend fun businessRegistered()
    suspend fun signOut()
    fun getFirebaseUser(): Flow<FirebaseUserDM?>
    fun getUser(): Flow<User?>
    fun getUserSync(): User?
    fun getJwtToken(refresh: Boolean = false): String?
    fun isAuthenticated(): Boolean
    fun sessionResolved(): Flow<Boolean> = flowOf(true)
    fun setPremium(premium: Boolean)
    suspend fun deleteAccount(token: String): Boolean
}
