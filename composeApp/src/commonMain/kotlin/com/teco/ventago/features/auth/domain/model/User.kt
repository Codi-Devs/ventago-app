package com.teco.ventago.features.auth.domain.model

import com.teco.ventago.features.auth.domain.model.response.AuthResponse
import com.teco.ventago.features.auth.domain.model.response.BusinessIds

data class User(
    val uid: String,
    val email: String,
    val name: String,
    val premium: Boolean,
    val active: Boolean,
    val missingBusiness: Boolean,
    val userId: Int,
    val businessIds: List<BusinessIds>
) {
    companion object {
        fun fromAuthResponse(authResponse: AuthResponse): User {
            return User(
                authResponse.uid,
                authResponse.email,
                authResponse.name,
                authResponse.premium,
                authResponse.active,
                authResponse.missingBusiness,
                authResponse.userId,
                authResponse.businesses
            )
        }
    }
}