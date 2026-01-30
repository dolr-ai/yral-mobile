package com.yral.shared.iap.utils

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable

@Composable
actual fun getPurchaseContext(): PurchaseContext? = LocalActivity.current?.let { AndroidPurchaseContext(it) }

actual fun PurchaseContext?.toPlatformContext(): Any? = (this as? AndroidPurchaseContext)?.activity
