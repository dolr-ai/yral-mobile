package com.yral.shared.iap.utils

/**
 * Platform-agnostic interface for purchase context.
 * - Android: Wraps [android.app.Activity] for billing flow
 * - iOS: Not used (returns null)
 */
interface PurchaseContext
