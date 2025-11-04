package com.teco.ventago.features.auth.data.provider

import androidx.compose.runtime.Composable
import com.teco.ventago.features.auth.domain.GoogleAuthUiProvider
import com.teco.ventago.features.auth.domain.model.GoogleUser


interface GoogleAuthProvider {
    suspend fun signIn(): GoogleUser?
    suspend fun signOut()
}

expect fun getGoogleAuthProvider(): GoogleAuthProvider