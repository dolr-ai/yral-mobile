package com.yral.shared.iap.utils

/**
 * Platform-agnostic interface for purchase context.
 * - Android: Wraps [android.app.Activity] for billing flow
 * - iOS: Marker object (StoreKit needs no host context)
 */
interface PurchaseContext
