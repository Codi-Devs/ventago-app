package com.teco.ventago.features.auth.data.provider

import com.teco.ventago.features.auth.domain.model.requests.CreateUserRequest
import com.teco.ventago.features.auth.domain.model.requests.EmailLoginRequest
import com.teco.ventago.utils.ApiResponse


interface IAuthProvider {
    suspend fun userExists(email: String): ApiResponse
    suspend fun googleLogin(googleToken: String, businessId: Int? = null): ApiResponse
    suspend fun emailLogin(request: EmailLoginRequest): ApiResponse
    suspend fun emailRegister(request: CreateUserRequest): ApiResponse
    suspend fun forgotPassword(email: String): ApiResponse
    suspend fun deleteAccount(token: String): ApiResponse
}