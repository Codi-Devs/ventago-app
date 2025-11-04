package com.teco.ventago.features.auth.domain.model.firebase

data class FirebaseUserDM(
    val uid: String,
    val displayName: String,
    val email: String,
    val phoneNumber: String,
    val photoUrl: String,
    val isEmailVerified: Boolean,
    val providerId: String,
    val providerData: List<String>
)