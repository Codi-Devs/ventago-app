package com.teco.ventago.features.auth.domain

import com.teco.ventago.features.auth.domain.model.User
import com.teco.ventago.features.auth.domain.model.firebase.FirebaseUserDM
import com.teco.ventago.features.auth.domain.model.requests.CreateUserRequest
import com.teco.ventago.features.auth.domain.model.requests.EmailLoginRequest
import com.teco.ventago.features.auth.domain.model.response.AuthResponse
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.Flow

interface IAuthService {
    suspend fun refreshToken(client: HttpClient)
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
    fun setPremium(premium: Boolean)
    suspend fun deleteAccount(token: String): Boolean
}