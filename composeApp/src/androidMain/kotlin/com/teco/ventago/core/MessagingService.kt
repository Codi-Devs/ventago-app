package com.teco.ventago.core

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService

class MessagingService: FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // FCM registration token to your app server.
        val mAuth = FirebaseAuth.getInstance()
        val user = mAuth.currentUser
        if (user != null) {
            val firestoredb = FirebaseFirestore.getInstance()
            val reference = firestoredb.collection("device_tokens")
                .document(user.uid)
            val data: MutableMap<String?, Any?> = HashMap()
            data.put("tokens", FieldValue.arrayUnion(token))
            reference.set(data, SetOptions.merge())
        }
        // Handle the new token here, e.g., send it to your server
    }

    override fun onMessageReceived(remoteMessage: com.google.firebase.messaging.RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        // Handle incoming messages here
    }


    companion object {
        fun saveToken(token: String) {
            val mAuth = FirebaseAuth.getInstance()
            val user = mAuth.currentUser
            if (user != null) {
                val firestoredb = FirebaseFirestore.getInstance()
                val reference = firestoredb.collection("device_tokens")
                    .document(user.uid)
                val data: MutableMap<String?, Any?> = java.util.HashMap<String?, Any?>()
                data.put("tokens", FieldValue.arrayUnion(token))
                reference.set(data, SetOptions.merge())
            }
        }
    }

}