package com.yral.shared.analytics.events

/**
 * Returns the platform-specific payment provider for analytics tracking.
 * - Android: Returns [PaymentProvider.GOOGLE]
 * - iOS: Returns [PaymentProvider.APPLE]
 */
expect fun getPlatformPaymentProvider(): PaymentProvider
