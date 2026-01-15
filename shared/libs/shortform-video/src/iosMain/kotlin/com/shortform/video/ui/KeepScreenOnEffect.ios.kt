package com.shortform.video.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import platform.UIKit.UIApplication

@Composable
actual fun KeepScreenOnEffect(keepScreenOn: Boolean) {
    DisposableEffect(keepScreenOn) {
        val application = UIApplication.sharedApplication
        val previous = application.idleTimerDisabled
        application.idleTimerDisabled = keepScreenOn
        onDispose { application.idleTimerDisabled = previous }
    }
}
