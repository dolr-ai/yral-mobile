package com.yral.android

import android.content.Intent
import androidx.annotation.WorkerThread
import androidx.core.net.toUri
import co.touchlab.kermit.Logger
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.yral.featureflag.FeatureFlagManager
import com.yral.featureflag.WalletFeatureFlags
import com.yral.shared.features.auth.domain.useCases.RegisterNotificationTokenUseCase
import com.yral.shared.koin.koinInstance
import com.yral.shared.libs.designsystem.component.toast.ToastManager
import com.yral.shared.libs.designsystem.component.toast.ToastStatus
import com.yral.shared.libs.designsystem.component.toast.ToastType
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
            "onMessageReceived: ${message.data}"
        }

        if (message.notification != null) {
            // notification messages are received here only when app is in foreground
            showToastForForegroundMessage(message)
        }

        super.onMessageReceived(message)
    }

    private fun showToastForForegroundMessage(message: RemoteMessage) {
        val notification = message.notification
        notification?.let {
            val data = message.data
            if (data.contains("rewards_received_bs") && data["rewards_received_bs"].toBoolean()) {
                handleBtcRewardsNotification(notification)
            } else {
                handleToastNotification(notification)
            }
        }
    }

    private fun handleToastNotification(notification: RemoteMessage.Notification) {
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

    private fun handleBtcRewardsNotification(notification: RemoteMessage.Notification) {
        val featureFlagManager: FeatureFlagManager = koinInstance.get()
        val link =
            notification.link ?: featureFlagManager.get(WalletFeatureFlags.Wallet.BtcRewardsLink).toUri()
        val intent = Intent(this, MainActivity::class.java)
        // Use these flags to bring an existing instance to the front or create a new one if needed
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        intent.data = link
        intent.putExtra("branch_force_new_session", true)
        startActivity(intent)
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
