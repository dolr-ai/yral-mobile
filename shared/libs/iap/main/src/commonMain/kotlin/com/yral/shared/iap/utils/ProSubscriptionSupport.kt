package com.yral.shared.iap.utils

/**
 * Whether the YRAL Pro subscription flow is available on this platform.
 * iOS has no backend verification endpoint for it yet, so Pro stays off there.
 */
expect fun isProSubscriptionSupported(): Boolean
