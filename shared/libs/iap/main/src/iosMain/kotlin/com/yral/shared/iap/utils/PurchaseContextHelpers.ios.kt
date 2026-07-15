package com.yral.shared.iap.utils

import androidx.compose.runtime.Composable

private object IosPurchaseContext : PurchaseContext

@Composable
actual fun getPurchaseContext(): PurchaseContext? = IosPurchaseContext

actual fun PurchaseContext?.toPlatformContext(): Any? = null
