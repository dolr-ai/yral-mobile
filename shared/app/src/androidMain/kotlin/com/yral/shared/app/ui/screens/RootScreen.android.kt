package com.yral.shared.app.ui.screens

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
internal actual fun HandleSystemBars(show: Boolean) {
    val window = (LocalActivity.current as? ComponentActivity)?.window
    val controller =
        window?.let { w ->
            WindowInsetsControllerCompat(
                w,
                w.decorView,
            )
        }
    controller?.isAppearanceLightStatusBars = false
    LaunchedEffect(Unit) {
        if (show) {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        } else {
            controller?.hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}
