package com.yral.shared.features.feed.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import platform.UIKit.UIApplication

@Composable
internal actual fun KeepScreenOnEffect(keepScreenOn: Boolean) {
    DisposableEffect(keepScreenOn) {
        val application = UIApplication.sharedApplication
        val previous = application.idleTimerDisabled
        application.idleTimerDisabled = keepScreenOn
        onDispose { application.idleTimerDisabled = previous }
    }
}
