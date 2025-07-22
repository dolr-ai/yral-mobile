package com.yral.android

import androidx.annotation.WorkerThread
import co.touchlab.kermit.Logger
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.yral.android.ui.components.ToastManager
import com.yral.android.ui.components.ToastStatus
import com.yral.android.ui.components.ToastType
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
        Logger.d("MyFirebaseMessagingService") {
            "onMessageReceived: ${message.notification?.clickAction}"
        }

        if (message.notification != null) {
            // notification messages are received here only when app is in foreground
            showToastForForegroundMessage(message)
        }

        super.onMessageReceived(message)
    }

    private fun showToastForForegroundMessage(message: RemoteMessage) {
        val notification = message.notification
        if (notification != null) {
            val title = notification.title
            val body = notification.body
            val toastType =
                if (title != null && body != null) {
                    ToastType.Big(title, body)
                } else {
                    val message = title ?: body
                    if (message != null) {
                        ToastType.Small(message)
                    } else {
                        return
                    }
                }

            ToastManager.showToast(
                type = toastType,
                status = ToastStatus.Success, // currently we don't have any information about status
            )
        }
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
