package com.teco.ventago.features.auth.domain

import com.teco.ventago.features.auth.domain.model.firebase.CustomTokenResult
import com.teco.ventago.features.auth.domain.model.firebase.FirebaseUserDM
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.AuthResult
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.stateIn

class FirebaseService: IFirebaseService {

    override suspend fun signInWithCustomToken(token: String): CustomTokenResult {
        return try {
            val auth = Firebase.auth
            val res: AuthResult = auth.signInWithCustomToken(token)

            res.user?.let {
                auth.updateCurrentUser(it)
                CustomTokenResult(true, it.uid)
            } ?: CustomTokenResult(false, "")
        } catch (e: Exception) {
            e.printStackTrace()
            CustomTokenResult(false, "")
        }
    }

    override fun getUser(): Flow<FirebaseUserDM?> = flow {
        val auth = Firebase.auth
        auth.authStateChanged
            .catch {
                emit(null)
            }
            .collect{
                if (it == null) {
                    emit(null)
                } else {
                    emit(it.toFirebaseUserDM())
                }
            }
    }

    override suspend fun signOut() {
        val auth = Firebase.auth
        if (auth.currentUser != null) {
            auth.signOut()
        }
    }

    override suspend fun sendPasswordResetEmail(email: String) {
        val auth = Firebase.auth
        auth.sendPasswordResetEmail(email)
    }


    //  Just playing around
    private fun flow(viewModelScope: CoroutineScope): Flow<FirebaseUser?> = callbackFlow {
        val auth = Firebase.auth
        auth.authStateChanged.collect {
            trySend(it)
        }

        auth.authStateChanged.last()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), Firebase.auth.currentUser)


}


fun FirebaseUser.toFirebaseUserDM(): FirebaseUserDM {
    return FirebaseUserDM(
        uid = uid,
        displayName = displayName ?: "",
        email = email ?: "",
        phoneNumber = phoneNumber ?: "",
        photoUrl = photoURL ?: "",
        isEmailVerified = isEmailVerified,
        providerId = providerId,
        providerData = providerData.map { data -> data.providerId }
    )
}
