package com.teco.ventago.features.auth.domain

import com.teco.ventago.features.auth.domain.model.firebase.CustomTokenResult
import com.teco.ventago.features.auth.domain.model.firebase.FirebaseUserDM
import kotlinx.coroutines.flow.Flow

interface IFirebaseService {
    suspend fun signInWithCustomToken(token: String): CustomTokenResult
    fun getUser(): Flow<FirebaseUserDM?>
    suspend fun signOut()
    suspend fun sendPasswordResetEmail(email: String)
    suspend fun deleteAccount()
}