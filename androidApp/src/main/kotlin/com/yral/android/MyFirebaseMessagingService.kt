package com.yral.android

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
        sendTokenToServer(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Logger.d("MyFirebaseMessagingService") { "onMessageReceived: $message" }
        super.onMessageReceived(message)
    }

    private fun sendTokenToServer(token: String) =
        runBlocking {
            val result =
                registerNotificationTokenUseCase(
                    RegisterNotificationTokenUseCase.Parameter(token = token),
                )
            Logger.d("MyFirebaseMessagingService") { "Notification token registered: $result" }
        }
}
