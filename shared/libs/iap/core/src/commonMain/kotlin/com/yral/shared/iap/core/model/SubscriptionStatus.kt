package com.yral.shared.iap.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class SubscriptionStatus {
    ACTIVE,
    PAUSED,
    CANCELLED,
    EXPIRED,
    UNKNOWN,
}
