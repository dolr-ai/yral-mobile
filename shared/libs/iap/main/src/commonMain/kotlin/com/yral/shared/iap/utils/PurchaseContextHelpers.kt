package com.yral.shared.iap.utils

import androidx.compose.runtime.Composable

/**
 * Retrieves the platform-specific purchase context.
 * Android: Returns [PurchaseContext] wrapping current [android.app.Activity]
 * iOS: Returns null
 */
@Composable
expect fun getPurchaseContext(): PurchaseContext?

/**
 * Extracts platform-specific context value for core IAP provider.
 * Android: Returns wrapped [android.app.Activity] or null
 * iOS: Returns null
 */
expect fun PurchaseContext?.toPlatformContext(): Any?
