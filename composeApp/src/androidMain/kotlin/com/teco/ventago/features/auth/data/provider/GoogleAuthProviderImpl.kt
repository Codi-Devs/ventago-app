package com.teco.ventago.features.auth.data.provider

import com.teco.ventago.shared.R
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.teco.ventago.features.auth.domain.model.GoogleUser
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import org.koin.java.KoinJavaComponent

internal class GoogleAuthProviderImpl(
    private val credentialManager: CredentialManager,
    private val context: Context,
): GoogleAuthProvider {

    override suspend fun signIn(): GoogleUser? {
        return try {
            val credential = credentialManager.getCredential(
                context = context,
                request = getCredentialRequest()
            ).credential
            getGoogleUserFromCredential(credential)
        } catch (e: GetCredentialException) {
            e.printStackTrace()
//            AppLogger.e("GoogleAuthUiProvider error: ${e.message}")
            null
        } catch (e: NullPointerException) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun signOut() {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            val auth = Firebase.auth
            if (auth.currentUser != null) {
                auth.signOut()
            }
        } catch (_: Exception) {
        }

    }

    private fun getGoogleUserFromCredential(credential: Credential): GoogleUser? {
        return when {
            credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                try {
                    val googleIdTokenCredential =
                        GoogleIdTokenCredential.createFrom(credential.data)
                    GoogleUser(
                        idToken = googleIdTokenCredential.idToken,
                        displayName = googleIdTokenCredential.displayName ?: "",
                        profilePicUrl = googleIdTokenCredential.profilePictureUri?.toString()
                    )
                } catch (e: GoogleIdTokenParsingException) {
                    e.printStackTrace()
//                    AppLogger.e("GoogleAuthUiProvider Received an invalid google id token response: ${e.message}")
                    null
                }
            }

            else -> null
        }
    }


    private fun getCredentialRequest(): GetCredentialRequest {
        return GetCredentialRequest.Builder()
            .addCredentialOption(getGoogleIdOption(serverClientId = context.getString(R.string.default_web_client_ids)))
            .build()
    }

    private fun getGoogleIdOption(serverClientId: String): GetGoogleIdOption {
        return GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(true)
            .setServerClientId(serverClientId)
            .build()
    }
}

actual fun getGoogleAuthProvider(): GoogleAuthProvider {
    val activity: ComponentActivity = KoinJavaComponent.getKoin().get()
    val credentialManager = CredentialManager.create(activity)
    return GoogleAuthProviderImpl(credentialManager, activity)
}
