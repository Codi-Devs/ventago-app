package com.teco.ventago.features.user.data.repository

import com.teco.ventago.features.auth.domain.model.response.AuthResponse


interface IUserRepository {
    suspend fun setPremium(premium: Boolean): Boolean
    suspend fun deleteAccount(token: String): Boolean
    suspend fun getUserData(token: String): AuthResponse
}