package com.yral.android

import co.touchlab.kermit.Logger
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        Logger.d("MyFirebaseMessagingService") { "onNewToken: $token" }
        super.onNewToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Logger.d("MyFirebaseMessagingService") { "onMessageReceived: $message" }
        super.onMessageReceived(message)
    }
}
