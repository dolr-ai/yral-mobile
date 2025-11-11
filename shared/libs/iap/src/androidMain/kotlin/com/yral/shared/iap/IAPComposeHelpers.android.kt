package com.yral.shared.iap

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun getPurchaseContext(): Any? {
    val context = LocalContext.current
    return context as? Activity
}
