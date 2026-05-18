package com.yral.shared.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.yral.shared.features.uploadvideo.presentation.VideoDraftPollingManager
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue

private const val DRAFT_CREATED_NOTIFICATION = "YralDraftCreatedNotification"

@Composable
internal actual fun ObserveDraftCreatedNotifications(videoDraftPollingManager: VideoDraftPollingManager) {
    DisposableEffect(videoDraftPollingManager) {
        val observer =
            NSNotificationCenter.defaultCenter.addObserverForName(
                name = DRAFT_CREATED_NOTIFICATION,
                `object` = null,
                queue = NSOperationQueue.mainQueue,
            ) {
                videoDraftPollingManager.onDraftCreatedNotification()
            }

        onDispose {
            NSNotificationCenter.defaultCenter.removeObserver(observer)
        }
    }
}
