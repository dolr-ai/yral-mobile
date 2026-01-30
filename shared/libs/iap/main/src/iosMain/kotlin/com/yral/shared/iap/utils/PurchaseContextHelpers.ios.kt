package com.yral.shared.iap.utils

import androidx.compose.runtime.Composable

@Composable
actual fun getPurchaseContext(): PurchaseContext? = null

actual fun PurchaseContext?.toPlatformContext(): Any? = null
