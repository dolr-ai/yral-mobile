package com.yral.shared.iap.model

import kotlinx.serialization.Serializable

/**
 * Subscription lifecycle status.
 *
 * Documentation: `shared/libs/iap/docs/IAP_README.md`
 */
@Serializable
enum class SubscriptionStatus {
    ACTIVE,
    PAUSED,
    CANCELLED,
    EXPIRED,
    UNKNOWN,
}
