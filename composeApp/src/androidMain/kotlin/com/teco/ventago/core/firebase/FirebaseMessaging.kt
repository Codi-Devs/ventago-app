package com.teco.ventago.core.firebase

import com.google.firebase.messaging.FirebaseMessaging
import com.teco.ventago.core.MessagingService

actual fun getToken() {
    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
        if (!task.isSuccessful) {
            return@addOnCompleteListener
        }
        val token = task.result
        if (token != null) {
            MessagingService.saveToken(token)
        }
    }
}