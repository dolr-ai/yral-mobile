package com.yral.shared.rust.service.domain.models

sealed class SubscriptionPlan {
    data class Pro(
        val subscription: YralProSubscription,
    ) : SubscriptionPlan()
    object Free : SubscriptionPlan()
}
