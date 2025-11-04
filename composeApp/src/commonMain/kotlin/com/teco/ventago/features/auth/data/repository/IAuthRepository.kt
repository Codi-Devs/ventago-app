package com.teco.ventago.features.auth.data.repository

import com.teco.ventago.features.auth.domain.model.requests.CreateUserRequest
import com.teco.ventago.features.auth.domain.model.requests.EmailLoginRequest
import com.teco.ventago.features.auth.domain.model.response.AuthResponse


interface IAuthRepository {
    suspend fun googleLogin(googleToken: String): AuthResponse
    suspend fun emailLogin(request: EmailLoginRequest): AuthResponse
    suspend fun emailRegister(request: CreateUserRequest): AuthResponse
}