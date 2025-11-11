package com.yral.shared.iap

import androidx.compose.runtime.Composable

@Composable
internal actual fun getPurchaseContext(): Any? {
    // iOS doesn't need Activity context for purchases
    return Unit
}
