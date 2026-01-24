package com.yral.shared.iap.utils

import android.app.Activity

/**
 * Android-specific implementation of [PurchaseContext] that wraps an [Activity].
 *
 * This wrapper provides type-safe access to the Activity required for launching
 * Google Play Billing flows.
 */
internal class AndroidPurchaseContext(
    val activity: Activity,
) : PurchaseContext
