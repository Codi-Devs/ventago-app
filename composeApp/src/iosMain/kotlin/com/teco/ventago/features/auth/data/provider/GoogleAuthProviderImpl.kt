package com.teco.ventago.features.auth.data.provider

//import cocoapods.GoogleSignIn.GIDSignIn
import com.teco.ventago.features.auth.domain.model.GoogleUser
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIApplication
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class GoogleAuthProviderImpl  : GoogleAuthProvider {
    @OptIn(ExperimentalForeignApi::class)
    override suspend fun signIn(): GoogleUser? = suspendCoroutine { continuation ->
        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        if (rootViewController == null) continuation.resume(null)
        else {
            continuation.resume(null)
//            GIDSignIn.sharedInstance
//                .signInWithPresentingViewController(rootViewController) { gidSignInResult, nsError ->
//                    nsError?.let { println("Error While signing: $nsError") }
//                    val idToken = gidSignInResult?.user?.idToken?.tokenString
//                    val profile = gidSignInResult?.user?.profile
//                    if (idToken != null) {
//                        val googleUser = GoogleUser(
//                            idToken = idToken,
//                            displayName = profile?.name ?: "",
//                            profilePicUrl = profile?.imageURLWithDimension(320u)?.absoluteString
//                        )
//                        continuation.resume(googleUser)
//                    } else continuation.resume(null)
//                }
        }

    }

    override suspend fun signOut() {
        TODO("Not yet implemented")
    }

}

actual fun getGoogleAuthProvider(): GoogleAuthProvider {
    return GoogleAuthProviderImpl()
}