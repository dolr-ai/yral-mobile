package com.yral.android

import android.content.Intent
import androidx.annotation.WorkerThread
import co.touchlab.kermit.Logger
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.yral.shared.features.auth.domain.useCases.RegisterNotificationTokenUseCase
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject

class MyFirebaseMessagingService : FirebaseMessagingService() {
    private val registerNotificationTokenUseCase: RegisterNotificationTokenUseCase by inject()

    @WorkerThread
    override fun onNewToken(token: String) {
        Logger.d("MyFirebaseMessagingService") { "onNewToken: $token" }
        sendTokenToServer()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Logger.d("MyFirebaseMessagingService") {
            "onMessageReceived: ${message.data}"
        }

        if (message.notification != null) {
            val handler =
                ForegroundNotificationHandler(
                    viewDraftsCtaText = getString(R.string.view_drafts),
                )
            handler.handle(
                title = message.notification?.title,
                body = message.notification?.body,
                data = message.data,
                onNavigate = { payload -> navigateToActivity(payload) },
            )
        }

        super.onMessageReceived(message)
    }

    private fun navigateToActivity(payload: String) {
        val intent = Intent(this, MainActivity::class.java)
        // Use these flags to bring an existing instance to the front or create a new one if needed
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        intent.putExtra("payload", payload)
        startActivity(intent)
    }

    private fun sendTokenToServer() =
        runBlocking {
            val result = registerNotificationTokenUseCase()
            Logger.d("MyFirebaseMessagingService") { "Notification token registered: $result" }
        }
}
