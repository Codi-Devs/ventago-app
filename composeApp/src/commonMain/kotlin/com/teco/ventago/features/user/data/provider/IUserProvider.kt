package com.teco.ventago.features.user.data.provider

import com.teco.ventago.utils.ApiResponse

interface IUserProvider {
    suspend fun setPremium(premium: Boolean): ApiResponse
    suspend fun getUserData(token: String): ApiResponse
}