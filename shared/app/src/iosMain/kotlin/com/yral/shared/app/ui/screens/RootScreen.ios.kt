package com.yral.shared.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.yral.shared.app.SystemBarsControllerHolder

@Composable
internal actual fun HandleSystemBars(show: Boolean) {
    LaunchedEffect(show) {
        SystemBarsControllerHolder.updateVisibility(show)
    }
}
