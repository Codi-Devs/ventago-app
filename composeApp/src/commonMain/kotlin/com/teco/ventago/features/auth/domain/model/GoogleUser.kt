package com.teco.ventago.features.auth.domain.model

data class GoogleUser(
    val idToken: String,
    val displayName: String = "",
    val profilePicUrl: String? = null,
)

data class GoogleAuthCredentials(val serverId: String)