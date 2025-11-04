package com.teco.ventago.features.auth.domain

import com.teco.ventago.features.auth.domain.model.GoogleUser

interface GoogleAuthUiProvider {

    /**
     * Opens Sign In with Google UI,
     * @return returns GoogleUser
     */
    suspend fun signIn(): GoogleUser?
}